<#
=====================================================================
 商城并发专项测试脚本（抢券不超卖 / 下单不超卖）
 前置条件：MySQL、Redis、后端(8080) 均已启动（见《商城接口测试指南.md》）
 用法： powershell -ExecutionPolicy Bypass -File mall-concurrency-test.ps1
 说明：
   - 脚本只新增测试数据（券类型/活动、商品、stressN 用户），不修改/删除已有数据
   - 每次运行都创建全新活动与商品，可重复执行
=====================================================================
#>

$BASE        = 'http://localhost:8080'
$MYSQL       = 'F:\mysql\mysql-9.6.0-winx64\bin\mysql.exe'
$MYSQL_ARGS  = @('-uroot', '-p123456wh', '--default-character-set=utf8mb4', 'big_event', '-N', '-B', '-e')
$STOCK_TOTAL = 3     # 券活动库存
$USERS       = 8     # 并发用户数（每人限抢 1 张，>库存数才能验证不超卖）
$STAMP       = Get-Date -Format 'MMddHHmmss'
$failCount   = 0

function Login([string]$u, [string]$p) {
    $r = Invoke-RestMethod -Method Post -Uri "$BASE/user/login?username=$u&password=$p"
    if ($r.code -ne 0) { throw "登录失败 ${u}: $($r.message)" }
    return $r.data
}

function Api([string]$method, [string]$path, $body, [string]$token) {
    $p = @{ Method = $method; Uri = "$BASE$path"; Headers = @{} }
    if ($token) { $p.Headers['Authorization'] = $token }
    if ($null -ne $body) {
        $json = $body | ConvertTo-Json -Depth 8
        $p.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
        $p.ContentType = 'application/json; charset=utf-8'
    }
    return Invoke-RestMethod @p
}

function Check([string]$name, [bool]$cond, [string]$detail = '') {
    if ($cond) {
        Write-Host ("  [PASS] " + $name) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] " + $name + "  ->  " + $detail) -ForegroundColor Red
        $script:failCount++
    }
}

function Sql([string]$sql) {
    & $MYSQL @MYSQL_ARGS $sql
}

Write-Host "`n=== 0. 环境自检 ===" -ForegroundColor Cyan
try {
    $null = Invoke-RestMethod -Method Get -Uri "$BASE/product?pageNum=1&pageSize=1"
    Write-Host "  后端 8080 可访问"
} catch {
    Write-Host "  后端不可访问：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------------
# 准备并发用户（stress1..stressN，密码 123456，不存在则注册）
# ---------------------------------------------------------------------
Write-Host "`n=== 1. 准备 $USERS 个并发用户 ===" -ForegroundColor Cyan
$tokens = @()
for ($i = 1; $i -le $USERS; $i++) {
    $u = "stress$i"
    try { $null = Api POST "/user/register?username=$u&password=123456" } catch { }
    try {
        $tokens += (Login $u '123456')
    } catch {
        Write-Host "  用户 $u 登录失败：$($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  已获取 $($tokens.Count) 个 token"

$TM = Login 't_merchant' '123456'

# =====================================================================
# A. 抢券并发：库存 $STOCK_TOTAL，$USERS 人同时抢 —— 成功数必须 = 库存
# =====================================================================
Write-Host "`n=== A. 并发抢券（库存 $STOCK_TOTAL / 并发 $USERS）===" -ForegroundColor Cyan

$typeName = "stress-coupon-$STAMP"
$null = Api POST '/merchant/coupon/type' `
    @{ name = $typeName; minSpend = 1; discountAmount = 1; validDays = 1 } $TM
$type = (Api GET '/coupon/type/list').data | Where-Object { $_.name -eq $typeName } | Select-Object -First 1
Check '创建券类型' ($null -ne $type) "未在 /coupon/type/list 找到 $typeName"
if ($null -eq $type) { exit 1 }

$null = Api POST '/merchant/coupon/stock' @{
    couponTypeId = $type.id
    totalCount   = $STOCK_TOTAL
    startTime    = (Get-Date).AddHours(-1).ToString('yyyy-MM-dd HH:mm:ss')
    endTime      = (Get-Date).AddDays(1).ToString('yyyy-MM-dd HH:mm:ss')
} $TM

$stock = (Api GET '/merchant/coupon/stock?pageNum=1&pageSize=50' $null $TM).data.items |
         Where-Object { $_.couponTypeId -eq $type.id } | Select-Object -First 1
Check '创建券活动' ($null -ne $stock) '未在 /merchant/coupon/stock 找到新活动'
if ($null -eq $stock) { exit 1 }
$stockId = $stock.id
Write-Host "  活动ID=$stockId 库存=$STOCK_TOTAL"

# 同时在 $startAt 时刻发起抢券
$startAt = (Get-Date).AddSeconds(4)
$jobs = foreach ($t in $tokens) {
    Start-Job -ScriptBlock {
        param($base, $token, $stockId, $startAt)
        while ((Get-Date) -lt $startAt) { Start-Sleep -Milliseconds 20 }
        try {
            $r = Invoke-RestMethod -Method Post -Uri "$base/coupon/stock/$stockId/grab" `
                    -Headers @{ Authorization = $token }
            [pscustomobject]@{ ok = ($r.code -eq 0); msg = $r.message }
        } catch {
            [pscustomobject]@{ ok = $false; msg = $_.Exception.Message }
        }
    } -ArgumentList $BASE, $t, $stockId, $startAt
}
Write-Host "  已发起 $($jobs.Count) 个并发请求，等待结果..."
$grabResults = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$okCount = @($grabResults | Where-Object { $_.ok }).Count
$liveRemain = (Api GET "/coupon/stock/$stockId").data.remainCount
$dbRemain = Sql "select remain_count from coupon_stock where id=$stockId"
$dbUsed = Sql "select count(*) from user_coupon where coupon_stock_id=$stockId"

Write-Host "  成功抢到：$okCount 张；接口实时剩余：$liveRemain；DB剩余：$dbRemain；DB领券记录：$dbUsed"
Check "抢券成功数 = 库存($STOCK_TOTAL)（不超卖）" ($okCount -eq $STOCK_TOTAL) "实际 $okCount"
Check '接口实时剩余 = 0' ($liveRemain -eq 0) "实际 $liveRemain"
Check '接口剩余数不为负' ($liveRemain -ge 0) "实际 $liveRemain"
Check '领券记录数 = 库存(不超发)' ($dbUsed -eq $STOCK_TOTAL) "实际 $dbUsed"
Write-Host "  [提示] DB剩余=$dbRemain 由 5 分钟对账任务与 Redis 对齐，不作为失败判定" -ForegroundColor DarkGray

# =====================================================================
# B. 下单并发：商品库存 1，$USERS 人同时下单 —— 只能成功 1 单
# =====================================================================
Write-Host "`n=== B. 并发下单（商品库存 1 / 并发 $USERS）===" -ForegroundColor Cyan

$prodName = "stress-product-$STAMP"
$null = Api POST '/product' @{
    name = $prodName; categoryId = 1
    coverImg = 'https://dummyimage.com/600x600/409eff/ffffff.png'
    price = 1.00; marketPrice = 1.00; stock = 1; status = 1
} $TM

$prod = (Api GET "/product/search?keyword=$prodName").data.items | Select-Object -First 1
Check '创建商品(库存1)' ($null -ne $prod) "未搜索到 $prodName"
if ($null -eq $prod) { exit 1 }
$prodId = $prod.id
Write-Host "  商品ID=$prodId 库存=1"

# 每个用户准备一个收货地址
$users = @()
for ($i = 0; $i -lt $tokens.Count; $i++) {
    $t = $tokens[$i]
    $list = (Api GET '/user/address/list' $null $t).data
    if ($null -eq $list -or @($list).Count -eq 0) {
        $null = Api POST '/user/address' @{
            receiverName = 'Stress'; receiverPhone = '13800138000'
            province = 'Guangdong'; city = 'Shenzhen'; district = 'Nanshan'
            detailAddress = 'Test Road 1'; isDefault = $true
        } $t
        $list = (Api GET '/user/address/list' $null $t).data
    }
    $users += [pscustomobject]@{ token = $t; addressId = @($list)[0].id }
}

$startAt2 = (Get-Date).AddSeconds(4)
$jobs2 = foreach ($u in $users) {
    Start-Job -ScriptBlock {
        param($base, $token, $addressId, $prodId, $startAt)
        while ((Get-Date) -lt $startAt) { Start-Sleep -Milliseconds 20 }
        $body = @{ orderType = 1; addressId = $addressId; remark = 'concurrency test'
                   productItems = @(@{ productId = $prodId; quantity = 1 }) } |
                ConvertTo-Json -Depth 6
        try {
            $r = Invoke-RestMethod -Method Post -Uri "$base/order" `
                    -Headers @{ Authorization = $token } `
                    -ContentType 'application/json; charset=utf-8' `
                    -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
            [pscustomobject]@{ ok = ($r.code -eq 0); msg = $r.message }
        } catch {
            [pscustomobject]@{ ok = $false; msg = $_.Exception.Message }
        }
    } -ArgumentList $BASE, $u.token, $u.addressId, $prodId, $startAt2
}
Write-Host "  已发起 $($jobs2.Count) 个并发下单，等待结果..."
$orderResults = $jobs2 | Wait-Job | Receive-Job
$jobs2 | Remove-Job

$okOrders = @($orderResults | Where-Object { $_.ok }).Count
$dbStock = Sql "select stock from product where id=$prodId"
$dbOrders = Sql "select count(*) from order_item where product_id=$prodId"
$dbSales = Sql "select sales_count from product where id=$prodId"

Write-Host "  下单成功：$okOrders 单；DB商品剩余库存：$dbStock；DB销量：$dbSales；DB订单数：$dbOrders"
Check '下单成功数 = 1（不超卖）' ($okOrders -eq 1) "实际 $okOrders"
Check 'DB商品库存 = 0' ($dbStock -eq 0) "实际 $dbStock"
Check 'DB销量 = 1' ($dbSales -eq 1) "实际 $dbSales"

# =====================================================================
Write-Host "`n=== 测试结束 ===" -ForegroundColor Cyan
if ($failCount -eq 0) {
    Write-Host "全部通过" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$failCount 项失败，详见上方 [FAIL]" -ForegroundColor Red
    exit 2
}
