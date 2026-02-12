<script setup lang="ts">
import { provide } from 'vue';
import Tooltip from './tooltip.vue';

interface Props {
  delayDuration?: number;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  delayDuration: 0,
});
</script>

<template>
  <Tooltip :delay-duration="delayDuration" v-bind="$attrs">
    <slot />
  </Tooltip>
</template>






