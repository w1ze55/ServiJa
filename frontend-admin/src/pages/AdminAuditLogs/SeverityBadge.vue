<script setup lang="ts">
import { computed } from 'vue'
import type { AuditSeverity } from '@/api/services/auditLogService'
import { SEVERITY_BADGE_STYLES, SEVERITY_LABELS } from './severity'

const props = defineProps<{
  severity: AuditSeverity
}>()

const styles = computed(() => SEVERITY_BADGE_STYLES[props.severity])
const label = computed(() => SEVERITY_LABELS[props.severity])
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
      styles.badge,
    ]"
  >
    <span :class="['h-1.5 w-1.5 rounded-full', styles.dot]" />
    {{ label }}
  </span>
</template>
