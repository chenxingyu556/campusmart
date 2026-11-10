<template>
  <AppHeader v-if="!isBare" />
  <router-view />
  <AppFooter v-if="!isBare" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'

const route = useRoute()
const isBare = computed(() => route.meta.bare === true)
</script>
