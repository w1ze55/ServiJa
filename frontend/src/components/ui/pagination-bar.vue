<script setup lang="ts">
import { computed } from 'vue';
import Pagination from './pagination.vue';
import PaginationContent from './pagination-content.vue';
import PaginationItem from './pagination-item.vue';
import PaginationLink from './pagination-link.vue';
import PaginationPrevious from './pagination-previous.vue';
import PaginationNext from './pagination-next.vue';
import PaginationEllipsis from './pagination-ellipsis.vue';

interface Props {
  currentPage: number;
  totalPages: number;
  siblingCount?: number;
}

const props = withDefaults(defineProps<Props>(), {
  siblingCount: 1,
});

const emit = defineEmits<{
  (e: 'change', page: number): void;
}>();

const DOTS = '...';

const pages = computed<(number | string)[]>(() => {
  const total = props.totalPages;
  const current = props.currentPage;
  const siblings = props.siblingCount;

  // primeira + última + atual + 2*vizinhos + 2 reticências
  const totalNumbers = siblings * 2 + 5;
  if (total <= totalNumbers) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  const leftSibling = Math.max(current - siblings, 1);
  const rightSibling = Math.min(current + siblings, total);

  const showLeftDots = leftSibling > 2;
  const showRightDots = rightSibling < total - 1;

  const result: (number | string)[] = [];

  if (!showLeftDots && showRightDots) {
    const leftItemCount = siblings * 2 + 3;
    for (let i = 1; i <= leftItemCount; i += 1) result.push(i);
    result.push(DOTS, total);
  } else if (showLeftDots && !showRightDots) {
    const rightItemCount = siblings * 2 + 3;
    result.push(1, DOTS);
    for (let i = total - rightItemCount + 1; i <= total; i += 1) result.push(i);
  } else {
    result.push(1, DOTS);
    for (let i = leftSibling; i <= rightSibling; i += 1) result.push(i);
    result.push(DOTS, total);
  }

  return result;
});

const goTo = (page: number) => {
  if (page < 1 || page > props.totalPages || page === props.currentPage) {
    return;
  }
  emit('change', page);
};
</script>

<template>
  <Pagination v-if="totalPages > 1" class="mt-8">
    <PaginationContent>
      <PaginationItem>
        <PaginationPrevious
          href="#"
          :class="currentPage <= 1 ? 'pointer-events-none opacity-50' : 'cursor-pointer'"
          @click.prevent="goTo(currentPage - 1)"
        />
      </PaginationItem>

      <PaginationItem v-for="(page, index) in pages" :key="`${page}-${index}`">
        <PaginationEllipsis v-if="page === '...'" />
        <PaginationLink
          v-else
          href="#"
          class="cursor-pointer"
          :is-active="page === currentPage"
          @click.prevent="goTo(page as number)"
        >
          {{ page }}
        </PaginationLink>
      </PaginationItem>

      <PaginationItem>
        <PaginationNext
          href="#"
          :class="currentPage >= totalPages ? 'pointer-events-none opacity-50' : 'cursor-pointer'"
          @click.prevent="goTo(currentPage + 1)"
        />
      </PaginationItem>
    </PaginationContent>
  </Pagination>
</template>
