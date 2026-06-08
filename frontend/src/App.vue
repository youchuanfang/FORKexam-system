<template>
  <div id="app">
    <NavBar v-if="isLoggedIn" />
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import { authState, syncAuthFromStorage } from './utils/auth'

const route = useRoute()
const isLoggedIn = computed(() => {
  route.fullPath
  syncAuthFromStorage()
  return !!authState.token
})
</script>
