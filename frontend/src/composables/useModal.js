import { ref } from 'vue'

export function useModal() {
  const modalConfig = ref({
    show: false,
    title: '',
    message: '',
    type: 'info',
    confirmText: '确定',
    cancelText: '取消',
    showCancel: false,
    onConfirm: null,
    onCancel: null
  })

  const showAlert = (message, type = 'info', title = '') => {
    return new Promise((resolve) => {
      modalConfig.value = {
        show: true,
        title: title,
        message: message,
        type: type,
        confirmText: '确定',
        showCancel: false,
        onConfirm: () => {
          modalConfig.value.show = false
          resolve(true)
        },
        onCancel: () => {
          modalConfig.value.show = false
          resolve(false)
        }
      }
    })
  }

  const showConfirm = (message, title = '确认', type = 'warning') => {
    return new Promise((resolve) => {
      modalConfig.value = {
        show: true,
        title: title,
        message: message,
        type: type,
        confirmText: '确定',
        cancelText: '取消',
        showCancel: true,
        onConfirm: () => {
          modalConfig.value.show = false
          resolve(true)
        },
        onCancel: () => {
          modalConfig.value.show = false
          resolve(false)
        }
      }
    })
  }

  const showSuccess = (message, title = '成功') => {
    return showAlert(message, 'success', title)
  }

  const showError = (message, title = '错误') => {
    return showAlert(message, 'error', title)
  }

  const showWarning = (message, title = '警告') => {
    return showAlert(message, 'warning', title)
  }

  const showInfo = (message, title = '提示') => {
    return showAlert(message, 'info', title)
  }

  return {
    modalConfig,
    showAlert,
    showConfirm,
    showSuccess,
    showError,
    showWarning,
    showInfo
  }
}
