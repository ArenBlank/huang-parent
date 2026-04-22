import { defineStore } from 'pinia'

const formatDate = (date) => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

const createDefaultDraft = () => ({
  planId: null,
  planItemId: null,
  recordDate: formatDate(new Date()),
  durationMin: 30,
  calories: 200,
  feeling: '状态不错'
})

export const useTrainingStore = defineStore('training', {
  state: () => ({
    draft: createDefaultDraft()
  }),
  actions: {
    patchDraft(patch = {}) {
      Object.assign(this.draft, patch)
    },
    setPlanSelection(planId) {
      this.draft.planId = planId ?? null
    },
    setActionSelection(planItemId) {
      this.draft.planItemId = planItemId ?? null
    },
    resetAfterSubmit() {
      const currentPlanId = this.draft.planId
      const currentPlanItemId = this.draft.planItemId
      Object.assign(this.draft, createDefaultDraft(), {
        planId: currentPlanId,
        planItemId: currentPlanItemId
      })
    },
    resetWhenNoPlans() {
      Object.assign(this.draft, createDefaultDraft(), {
        planId: null,
        planItemId: null
      })
    }
  }
})
