<template>
  <div class="group-bar">
    <button
      :class="['group-tab', { active: activeGroup === null }]"
      @click="selectGroup(null)"
    >
      全部<span class="count">{{ totalCount }}</span>
    </button>
    <button
      v-for="group in groups"
      :key="group.id"
      :class="['group-tab', { active: activeGroup === group.id }]"
      @click="selectGroup(group.id)"
    >
      {{ group.groupName }}<span class="count">{{ group.fundCount }}</span>
    </button>
    <button class="group-tab add-group" @click="showInput = true" v-if="!showInput">
      + 新建分组
    </button>
    <div class="group-input" v-if="showInput">
      <input
        v-model="newGroupName"
        placeholder="分组名称"
        maxlength="20"
        @keydown.enter="handleCreate"
        @keydown.escape="cancelInput"
        ref="inputRef"
      />
      <button class="confirm-btn" @click="handleCreate">确定</button>
      <button class="cancel-btn" @click="cancelInput">取消</button>
    </div>
    <!-- 右键菜单占位（后期优化） -->
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useWatchlistStore } from '../stores/watchlistStore'

const store = useWatchlistStore()
const { groups, items } = storeToRefs(store)

const emit = defineEmits(['filter-change'])

const activeGroup = ref(null)
const showInput = ref(false)
const newGroupName = ref('')
const inputRef = ref(null)

const totalCount = computed(() => items.value.length)

function selectGroup(groupId) {
  activeGroup.value = groupId
  emit('filter-change', groupId)
}

async function handleCreate() {
  const name = newGroupName.value.trim()
  if (!name) return
  try {
    await store.createGroup(name)
    newGroupName.value = ''
    showInput.value = false
  } catch (err) {
    // error handled by store
  }
}

function cancelInput() {
  newGroupName.value = ''
  showInput.value = false
}
</script>

<style scoped>
.group-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  flex-wrap: wrap;
}

.group-tab {
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.group-tab:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.group-tab.active {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}

.group-tab .count {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.7;
}

.group-tab.add-group {
  border-style: dashed;
  color: #999;
}

.group-tab.add-group:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.group-input {
  display: flex;
  align-items: center;
  gap: 6px;
}

.group-input input {
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 16px;
  font-size: 13px;
  width: 120px;
  outline: none;
}

.group-input input:focus {
  border-color: #1677ff;
}

.confirm-btn,
.cancel-btn {
  padding: 4px 10px;
  border-radius: 12px;
  border: none;
  font-size: 12px;
  cursor: pointer;
}

.confirm-btn {
  background: #1677ff;
  color: #fff;
}

.cancel-btn {
  background: #f0f0f0;
  color: #666;
}
</style>
