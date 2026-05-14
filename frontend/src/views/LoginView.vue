<template>
  <Login @go-register="router.push('/register')" @login-success="router.push('/')" />
</template>

<script setup>
import { useRouter } from 'vue-router'
import Login from '../components/Login.vue'

const router = useRouter()
</script>
