import {defineStore} from 'pinia'
import {ref} from 'vue'
import {adminStatusService} from '@/api/admin.js'

//当前登录用户的角色状态（是否管理员）。
//不持久化：每次刷新/登录后由 Layout 重新拉取，退出登录时 reset，避免普通用户残留管理员标识
const useRoleInfoStore = defineStore('roleInfo', () => {
    const isAdmin = ref(false)
    const loaded = ref(false)

    const loadAdminStatus = async () => {
        try {
            const result = await adminStatusService()
            isAdmin.value = !!result.data && result.data.admin === true
        } catch (e) {
            isAdmin.value = false
        } finally {
            loaded.value = true
        }
    }

    const reset = () => {
        isAdmin.value = false
        loaded.value = false
    }

    return {isAdmin, loaded, loadAdminStatus, reset}
})

export default useRoleInfoStore
