import { ref, computed, onMounted, onUnmounted } from 'vue';

const MOBILE_BREAKPOINT = 768;

export function useIsMobile() {
  const isMobile = ref<boolean | undefined>(undefined);

  const checkMobile = () => {
    isMobile.value = window.innerWidth < MOBILE_BREAKPOINT;
  };

  let mql: MediaQueryList | null = null;
  let handleChange: (() => void) | null = null;

  onMounted(() => {
    mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`);
    handleChange = () => {
      checkMobile();
    };
    mql.addEventListener('change', handleChange);
    checkMobile();
  });

  onUnmounted(() => {
    if (mql && handleChange) {
      mql.removeEventListener('change', handleChange);
    }
  });

  return computed(() => !!isMobile.value);
}
