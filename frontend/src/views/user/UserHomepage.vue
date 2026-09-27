<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userHomepageService, userUpdateBioService } from '@/api/user.js'
import { communityUserArticlesService } from '@/api/community.js'
import { followService, unfollowService } from '@/api/follow.js'
import {
    likedArticlesService,
    favoriteArticlesService,
    myFavoriteFoldersService,
    createFavoriteFolderService,
    deleteFavoriteFolderService,
    articleUnfavoriteService,
    articleLikeService,
    articleUnlikeService
} from '@/api/article.js'
import { formatTimeShort } from '@/utils/mall.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { Close } from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()

const userId = computed(() => Number(route.params.userId))
const profile = ref(null)
const loading = ref(false)
const isValidId = computed(() => Number.isFinite(userId.value) && userId.value > 0)
const isSelf = computed(() => !!userInfoStore.info?.id && profile.value?.isSelf)

//关注
const followed = ref(false)
const followLoading = ref(false)

//自我介绍编辑（本人）
const bioDialog = ref(false)
const bioInput = ref('')
const bioSaving = ref(false)

// ===== 主页标签页：作品 / 赞过 / 收藏（赞过与收藏仅本人可见） =====
const activeTab = ref('works')

const articles = ref([])
const articleTotal = ref(0)
const articlePage = ref(1)
const articlePageSize = ref(10)
const articleLoading = ref(false)

const likedArticles = ref([])
const likedTotal = ref(0)
const likedPage = ref(1)
const likedLoading = ref(false)

const folders = ref([])//[{id:null|n, name, articleCount}]
const activeFolderId = ref(null)//null=默认收藏夹
const favArticles = ref([])
const favTotal = ref(0)
const favPage = ref(1)
const favLoading = ref(false)

//当前标签页对应的列表数据
const currentList = computed(() => activeTab.value === 'works' ? articles.value
    : activeTab.value === 'liked' ? likedArticles.value : favArticles.value)
const currentTotal = computed(() => activeTab.value === 'works' ? articleTotal.value
    : activeTab.value === 'liked' ? likedTotal.value : favTotal.value)
const currentLoading = computed(() => activeTab.value === 'works' ? articleLoading.value
    : activeTab.value === 'liked' ? likedLoading.value : favLoading.value)

const loadProfile = async () => {
    if (!isValidId.value) {
        profile.value = null
        return
    }
    loading.value = true
    try {
        const result = await userHomepageService(userId.value)
        profile.value = result.data || null
        followed.value = !!result.data?.followed
    } catch (e) {
        profile.value = null
    } finally {
        loading.value = false
    }
}

const loadArticles = async () => {
    if (!isValidId.value) {
        articles.value = []
        articleTotal.value = 0
        return
    }
    articleLoading.value = true
    try {
        const result = await communityUserArticlesService(userId.value, {
            pageNum: articlePage.value,
            pageSize: articlePageSize.value
        })
        articles.value = result.data?.items || []
        articleTotal.value = Number(result.data?.total || 0)
    } catch (e) {
        articles.value = []
        articleTotal.value = 0
    } finally {
        articleLoading.value = false
    }
}

const loadLiked = async () => {
    if (!isValidId.value) return
    likedLoading.value = true
    try {
        const result = await likedArticlesService(userId.value, {
            pageNum: likedPage.value,
            pageSize: articlePageSize.value
        })
        likedArticles.value = result.data?.items || []
        likedTotal.value = Number(result.data?.total || 0)
    } catch (e) {
        likedArticles.value = []
        likedTotal.value = 0
    } finally {
        likedLoading.value = false
    }
}

const loadFolders = async () => {
    if (!isValidId.value) return
    try {
        folders.value = (await myFavoriteFoldersService(userId.value).then(r => r.data)) || []
    } catch (e) {
        folders.value = []
    }
}

const loadFavorites = async () => {
    if (!isValidId.value) return
    favLoading.value = true
    try {
        const params = { pageNum: favPage.value, pageSize: articlePageSize.value }
        if (activeFolderId.value !== null) params.folderId = activeFolderId.value
        const result = await favoriteArticlesService(userId.value, params)
        favArticles.value = result.data?.items || []
        favTotal.value = Number(result.data?.total || 0)
    } catch (e) {
        favArticles.value = []
        favTotal.value = 0
    } finally {
        favLoading.value = false
    }
}

//赞过/收藏仅本人可见：非本人不加载
const loadSocialTabs = () => {
    if (!isSelf.value) return
    loadLiked()
    loadFolders()
    loadFavorites()
}

const selectFolder = (id) => {
    activeFolderId.value = id
    favPage.value = 1
    loadFavorites()
}

const createFolder = async () => {
    try {
        const { value } = await ElMessageBox.prompt('给收藏夹起个名字（1-30 字）', '新建收藏夹', {
            confirmButtonText: '创建',
            cancelButtonText: '取消',
            inputPattern: /^.{1,30}$/,
            inputErrorMessage: '名称须为 1-30 字'
        })
        await createFavoriteFolderService(value.trim())
        ElMessage.success('收藏夹已创建')
        loadFolders()
    } catch (e) {
        // 用户取消
    }
}

const removeFolder = (folder) => {
    ElMessageBox.confirm(
        `删除收藏夹「${folder.name}」后，夹内 ${folder.articleCount} 篇文章会回到默认收藏夹。确认删除？`,
        '删除收藏夹',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
        await deleteFavoriteFolderService(folder.id)
        ElMessage.success('收藏夹已删除')
        if (activeFolderId.value === folder.id) {
            activeFolderId.value = null
        }
        loadFolders()
        loadFavorites()
    }).catch(() => { })
}

const unfavorite = async (article) => {
    await articleUnfavoriteService(article.noteId)
    ElMessage.success('已取消收藏')
    loadFolders()
    loadFavorites()
}

const onPageChange = (num) => {
    if (activeTab.value === 'works') { articlePage.value = num; loadArticles() }
    else if (activeTab.value === 'liked') { likedPage.value = num; loadLiked() }
    else { favPage.value = num; loadFavorites() }
}

const onTabChange = (name) => {
    if (name === 'liked' && likedTotal.value === 0) loadLiked()
    if (name === 'favorites' && favTotal.value === 0) { loadFolders(); loadFavorites() }
}

//作品卡片点赞：与详情页同一套接口；乐观更新失败回滚，请求锁防连点
const likePendingIds = new Set()
const toggleWorkLike = async (a) => {
    if (!userInfoStore.info?.id) {
        ElMessage.warning('请先登录后点赞')
        return router.push('/login')
    }
    if (likePendingIds.has(a.noteId)) return
    likePendingIds.add(a.noteId)
    const prevLiked = !!a.liked
    const prevCount = Number(a.likeCount || 0)
    a.liked = !prevLiked
    a.likeCount = prevCount + (prevLiked ? -1 : 1)
    try {
        const result = prevLiked
            ? await articleUnlikeService(a.noteId)
            : await articleLikeService(a.noteId)
        if (result.data != null) a.likeCount = Number(result.data)
        // "赞过"页取消点赞后从列表移除，保持列表与状态一致
        if (activeTab.value === 'liked' && !a.liked) {
            const idx = likedArticles.value.findIndex(x => x.noteId === a.noteId)
            if (idx >= 0) {
                likedArticles.value.splice(idx, 1)
                likedTotal.value = Math.max(0, likedTotal.value - 1)
            }
        }
    } catch (e) {
        a.liked = prevLiked
        a.likeCount = prevCount
    } finally {
        likePendingIds.delete(a.noteId)
    }
}

const toggleFollow = async () => {
    if (!userInfoStore.info?.id) {
        ElMessage.warning('请先登录后再关注')
        return router.push('/login')
    }
    followLoading.value = true
    try {
        if (followed.value) {
            await unfollowService(1, userId.value)
            followed.value = false
            profile.value.followerCount = Math.max(0, profile.value.followerCount - 1)
            ElMessage.success('已取消关注')
        } else {
            await followService(1, userId.value)
            followed.value = true
            profile.value.followerCount += 1
            ElMessage.success('关注成功')
        }
    } finally {
        followLoading.value = false
    }
}

const openBioDialog = () => {
    bioInput.value = profile.value?.bio || ''
    bioDialog.value = true
}

const saveBio = async () => {
    bioSaving.value = true
    try {
        await userUpdateBioService({ bio: bioInput.value })
        profile.value.bio = bioInput.value
        ElMessage.success('自我介绍已更新')
        bioDialog.value = false
    } finally {
        bioSaving.value = false
    }
}

watch([userId, () => userInfoStore.info?.id], () => {
    articlePage.value = 1
    likedPage.value = 1
    favPage.value = 1
    activeTab.value = 'works'
    loadProfile()
    loadArticles()
    loadSocialTabs()
}, { immediate: true })
</script>

<template>
    <div class="homepage" v-loading="loading">
        <el-card class="head-card" v-if="profile">
            <div class="head-wrap">
                <el-avatar :size="88" :src="profile.avatar || avatar">
                    {{ (profile.nickname || profile.username || '匿')[0] }}
                </el-avatar>
                <div class="head-info">
                    <h2 class="name">{{ profile.nickname || profile.username }}</h2>
                    <div class="username">@{{ profile.username }}</div>
                    <div class="bio">{{ profile.bio || (isSelf ? '还没有自我介绍，点击右上角"编辑"写一句吧' : '这个人很懒，什么都没有留下') }}</div>
                    <div class="stats">
                        <span><b>{{ profile.articleCount }}</b> 作品</span>
                        <span><b>{{ profile.followerCount }}</b> 粉丝</span>
                        <span><b>{{ profile.followingCount }}</b> 关注</span>
                    </div>
                </div>
                <div class="head-actions">
                    <template v-if="isSelf">
                        <el-button type="primary" plain round @click="openBioDialog">编辑自我介绍</el-button>
                    </template>
                    <template v-else>
                        <el-button :type="followed ? 'default' : 'primary'" round :loading="followLoading"
                            @click="toggleFollow">
                            {{ followed ? '已关注' : '+ 关注' }}
                        </el-button>
                    </template>
                    <el-button v-if="profile.shopUserId" type="warning" round
                        @click="router.push(`/mall/shop/${profile.shopUserId}`)">
                        🏪 {{ profile.shopName }}
                    </el-button>
                </div>
            </div>

            <!-- 本人：我的店铺卡片 -->
            <el-alert v-if="isSelf && profile.shopUserId" type="warning" :closable="false" show-icon
                class="shop-tip" @click="router.push(`/mall/shop/${profile.shopUserId}`)">
                <template #title>
                    你已开通店铺「{{ profile.shopName }}」—— 点击进入店铺主页查看商品与粉丝
                </template>
            </el-alert>
            <el-alert v-else-if="isSelf && !profile.shopUserId" type="info" :closable="false" show-icon
                class="shop-tip" @click="router.push('/merchant/apply')">
                <template #title>你还没有开通店铺 —— 想卖东西？点击去申请商家入驻</template>
            </el-alert>
        </el-card>

        <el-empty v-else-if="!loading" :description="isValidId ? '用户不存在' : '主页参数有误，请从正确的入口进入'" />

        <el-card class="works-card" v-if="profile">
            <el-tabs v-model="activeTab" @tab-change="onTabChange">
                <el-tab-pane label="作品" name="works" />
                <el-tab-pane v-if="isSelf" label="赞过" name="liked" />
                <el-tab-pane v-if="isSelf" label="收藏" name="favorites" />
            </el-tabs>

            <!-- 收藏夹切换（本人"收藏"标签页） -->
            <div class="folder-bar" v-if="activeTab === 'favorites' && isSelf">
                <div class="folder-chips">
                    <span :class="['folder-chip', { active: activeFolderId === null }]"
                        @click="selectFolder(null)">📁 默认收藏夹</span>
                    <span v-for="f in folders" :key="f.id"
                        :class="['folder-chip', { active: activeFolderId === f.id }]"
                        @click="selectFolder(f.id)">
                        📁 {{ f.name }}
                        <el-icon class="folder-del" title="删除收藏夹" @click.stop="removeFolder(f)"><Close /></el-icon>
                    </span>
                    <el-button link type="primary" size="small" @click="createFolder">+ 新建收藏夹</el-button>
                </div>
            </div>

            <div v-loading="currentLoading">
                <div class="works-list" v-if="currentList.length">
                    <div class="work-item" v-for="a in currentList" :key="a.noteId"
                        @click="router.push(`/article/detail/${a.noteId}`)">
                        <img v-if="a.coverImage" :src="a.coverImage" class="work-cover" />
                        <div class="work-body">
                            <div class="work-title">{{ a.title }}</div>
                            <div class="work-summary">{{ a.summary }}</div>
                            <div class="work-meta">
                                <span>👁 {{ a.viewCount || 0 }}</span>
                                <span :class="['like-chip', { liked: a.liked }]"
                                    @click.stop="toggleWorkLike(a)">
                                    {{ a.liked ? '❤️' : '🤍' }} {{ a.likeCount || 0 }}
                                </span>
                                <span>⭐ {{ a.favoriteCount || 0 }}</span>
                                <span class="time">{{ formatTimeShort(a.createTime) }}</span>
                                <el-button v-if="activeTab === 'favorites'" type="danger" size="small" text
                                    @click.stop="unfavorite(a)">取消收藏</el-button>
                            </div>
                        </div>
                    </div>
                </div>
                <el-empty v-else :description="activeTab === 'works' ? '还没有发布过作品'
                    : activeTab === 'liked' ? '还没有赞过的文章' : '这个收藏夹还是空的'" />

                <el-pagination v-if="currentTotal > articlePageSize"
                    v-model:current-page="articlePage" :page-size="articlePageSize"
                    layout="prev, pager, next" background :total="currentTotal"
                    @current-change="onPageChange" style="margin-top: 16px; justify-content: center" />
            </div>
        </el-card>

        <!-- 编辑自我介绍 -->
        <el-dialog v-model="bioDialog" title="编辑自我介绍" width="440px">
            <el-input v-model="bioInput" type="textarea" :rows="4" maxlength="200" show-word-limit
                placeholder="介绍一下自己，让关注你的人更了解你（最多 200 字）" />
            <template #footer>
                <el-button @click="bioDialog = false">取消</el-button>
                <el-button type="primary" :loading="bioSaving" @click="saveBio">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.homepage {
    min-height: 100%;

    .head-card {
        margin-bottom: 20px;

        .head-wrap {
            display: flex;
            align-items: center;
            gap: 20px;
        }

        .head-info {
            flex: 1;
            min-width: 0;

            .name {
                margin: 0;
                font-size: 22px;
            }

            .username {
                color: #999;
                font-size: 13px;
                margin: 4px 0 8px;
            }

            .bio {
                color: #666;
                font-size: 14px;
                line-height: 1.6;
                margin-bottom: 10px;
            }

            .stats {
                display: flex;
                gap: 24px;
                color: #666;
                font-size: 14px;

                b {
                    color: #1f2329;
                    margin-right: 4px;
                }
            }
        }

        .head-actions {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .shop-tip {
            margin-top: 16px;
            cursor: pointer;
        }
    }

    .works-card {
        .folder-bar {
            margin-bottom: 14px;

            .folder-chips {
                display: flex;
                flex-wrap: wrap;
                align-items: center;
                gap: 10px;

                .folder-chip {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    padding: 6px 12px;
                    border: 1px solid #ebeef5;
                    border-radius: 16px;
                    font-size: 13px;
                    color: #666;
                    cursor: pointer;
                    user-select: none;

                    &:hover {
                        border-color: var(--el-color-primary);
                        color: var(--el-color-primary);
                    }

                    &.active {
                        background: var(--el-color-primary-light-9, #ecf5ff);
                        border-color: var(--el-color-primary);
                        color: var(--el-color-primary);
                    }

                    .folder-del {
                        font-size: 12px;
                        border-radius: 50%;

                        &:hover { color: #f56c6c; }
                    }
                }
            }
        }

        .works-list {
            .work-item {
                display: flex;
                gap: 14px;
                padding: 14px 0;
                border-bottom: 1px solid #f2f4f7;
                cursor: pointer;

                &:hover .work-title { color: var(--el-color-primary); }
                &:last-of-type { border-bottom: none; }

                .work-cover {
                    width: 120px;
                    height: 80px;
                    object-fit: cover;
                    border-radius: 8px;
                    background: #f5f7fa;
                    flex-shrink: 0;
                }

                .work-body {
                    flex: 1;
                    min-width: 0;

                    .work-title {
                        font-size: 16px;
                        font-weight: 600;
                        margin-bottom: 6px;
                    }

                    .work-summary {
                        color: #999;
                        font-size: 13px;
                        display: -webkit-box;
                        -webkit-line-clamp: 2;
                        -webkit-box-orient: vertical;
                        overflow: hidden;
                        margin-bottom: 8px;
                    }

                    .work-meta {
                        display: flex;
                        align-items: center;
                        gap: 16px;
                        color: #999;
                        font-size: 12px;

                        .time { color: #ccc; }

                        .like-chip {
                            cursor: pointer;
                            user-select: none;

                            &:hover,
                            &.liked { color: #f56c6c; }
                        }
                    }
                }
            }
        }
    }
}
</style>
