<template>
  <div id="app">
    <header class="app-header">
      <h1>基金实时估值与持仓管理系统</h1>
    </header>
    
    <main class="app-main">
      <!-- Search Section -->
      <section class="search-section">
        <SearchFund @add-fund="handleAddFund" />
      </section>
      
      <!-- Fund List Section -->
      <section class="list-section">
        <FundList 
          @delete-fund="handleDeleteFund" 
          @view-detail="handleViewDetail"
        />
      </section>
      
      <!-- Fund Detail Modal -->
      <FundDetailModal
        v-if="showDetail"
        :fund-code="currentFundCode"
        @close="closeDetail"
      />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SearchFund from './components/SearchFund.vue'
import FundList from './components/FundList.vue'
import FundDetailModal from './components/FundDetailModal.vue'

const showDetail = ref(false)
const currentFundCode = ref('')

const handleAddFund = () => {
  // Refresh list after adding
}

const handleDeleteFund = () => {
  // Refresh list after deleting
}

const handleViewDetail = (fundCode) => {
  currentFundCode.value = fundCode
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  currentFundCode.value = ''
}
</script>

<style scoped>
#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-section {
  margin-bottom: 20px;
}

.list-section {
  margin-bottom: 20px;
}
</style>