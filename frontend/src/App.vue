<template>
  <div id="app">
    <NavBar v-if="isLoggedIn" />
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import NavBar from './components/NavBar.vue'

const isLoggedIn = computed(() => !!localStorage.getItem('token'))
</script>
