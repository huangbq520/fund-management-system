<template>
  <Register @go-login="router.push('/login')" @register-success="router.push('/')" />
</template>

<script setup>
import { useRouter } from 'vue-router'
import Register from '../components/Register.vue'

const router = useRouter()
</script>
