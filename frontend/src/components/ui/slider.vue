<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue';
import { cn } from './utils';

interface Props {
  defaultValue?: number | number[];
  value?: number | number[];
  min?: number;
  max?: number;
  step?: number;
  disabled?: boolean;
  orientation?: 'horizontal' | 'vertical';
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  max: 100,
  step: 1,
  orientation: 'horizontal',
  disabled: false,
});

const emit = defineEmits<{
  'update:value': [value: number | number[]];
  'valueChange': [value: number | number[]];
}>();

const modelValue = computed({
  get: () => props.value ?? props.defaultValue ?? props.min,
  set: (val) => {
    emit('update:value', val);
    emit('valueChange', val);
  },
});

const internalValue = ref<number | number[]>(
  Array.isArray(modelValue.value) 
    ? [...modelValue.value] 
    : modelValue.value
);

watch(() => props.value, (newValue) => {
  if (newValue !== undefined) {
    internalValue.value = Array.isArray(newValue) ? [...newValue] : newValue;
  }
}, { immediate: true });

watch(() => props.defaultValue, (newValue) => {
  if (newValue !== undefined && props.value === undefined) {
    internalValue.value = Array.isArray(newValue) ? [...newValue] : newValue;
  }
}, { immediate: true });

const values = computed(() => {
  if (Array.isArray(internalValue.value)) {
    return internalValue.value;
  }
  if (Array.isArray(props.defaultValue)) {
    return props.defaultValue;
  }
  if (Array.isArray(props.value)) {
    return props.value;
  }
  return [props.min, props.max];
});

const rootClasses = computed(() =>
  cn(
    'relative flex w-full touch-none items-center select-none',
    'data-[disabled]:opacity-50',
    props.orientation === 'vertical' && 'h-full min-h-44 w-auto flex-col',
    props.class
  )
);

const trackClasses = computed(() =>
  cn(
    'bg-muted relative grow overflow-hidden rounded-full',
    props.orientation === 'horizontal' ? 'h-4 w-full' : 'h-full w-1.5'
  )
);

const rangeClasses = computed(() =>
  cn(
    'bg-primary absolute',
    props.orientation === 'horizontal' ? 'h-full' : 'w-full'
  )
);

const thumbClasses = 'border-primary bg-background ring-ring/50 block size-4 shrink-0 rounded-full border shadow-sm transition-[color,box-shadow] hover:ring-4 focus-visible:ring-4 focus-visible:outline-hidden disabled:pointer-events-none disabled:opacity-50';

const getPercentage = (val: number) => {
  return ((val - props.min) / (props.max - props.min)) * 100;
};

const rangeStyle = computed(() => {
  const vals = values.value;
  if (props.orientation === 'horizontal') {
    if (vals.length === 2) {
      const min = Math.min(vals[0] as number, vals[1] as number);
      const max = Math.max(vals[0] as number, vals[1] as number);
      return {
        left: `${getPercentage(min)}%`,
        width: `${getPercentage(max) - getPercentage(min)}%`,
      };
    }
    return {
      left: '0%',
      width: `${getPercentage(vals[0] as number)}%`,
    };
  } else {
    if (vals.length === 2) {
      const min = Math.min(vals[0] as number, vals[1] as number);
      const max = Math.max(vals[0] as number, vals[1] as number);
      return {
        bottom: `${getPercentage(min)}%`,
        height: `${getPercentage(max) - getPercentage(min)}%`,
      };
    }
    return {
      bottom: '0%',
      height: `${getPercentage(vals[0] as number)}%`,
    };
  }
});

const thumbStyles = computed(() => {
  return values.value.map((val) => {
    const percentage = getPercentage(val as number);
    if (props.orientation === 'horizontal') {
      return { left: `${percentage}%`, transform: 'translateX(-50%)' };
    } else {
      return { bottom: `${percentage}%`, transform: 'translateY(50%)' };
    }
  });
});

let dragState: {
  index: number;
  startValue: number;
  startPos: number;
  trackElement: HTMLElement;
} | null = null;

const getValueFromPosition = (pos: number, trackElement: HTMLElement) => {
  if (props.orientation === 'horizontal') {
    const rect = trackElement.getBoundingClientRect();
    const percentage = Math.max(0, Math.min(1, (pos - rect.left) / rect.width));
    const rawValue = props.min + percentage * (props.max - props.min);
    return Math.round(rawValue / props.step) * props.step;
  } else {
    const rect = trackElement.getBoundingClientRect();
    const percentage = Math.max(0, Math.min(1, 1 - (pos - rect.top) / rect.height));
    const rawValue = props.min + percentage * (props.max - props.min);
    return Math.round(rawValue / props.step) * props.step;
  }
};

const handleMouseDown = (e: MouseEvent, index: number) => {
  if (props.disabled) return;
  e.preventDefault();
  
  const trackElement = (e.currentTarget as HTMLElement).closest('[data-slot="slider"]')?.querySelector('[data-slot="slider-track"]') as HTMLElement;
  if (!trackElement) return;

  const currentValues = Array.isArray(internalValue.value) 
    ? [...internalValue.value] 
    : [internalValue.value as number];
  
  dragState = {
    index,
    startValue: currentValues[index] as number,
    startPos: props.orientation === 'horizontal' ? e.clientX : e.clientY,
    trackElement,
  };

  const handleMouseMove = (moveEvent: MouseEvent) => {
    if (!dragState) return;
    
    const newValue = getValueFromPosition(
      props.orientation === 'horizontal' ? moveEvent.clientX : moveEvent.clientY,
      dragState.trackElement
    );
    
    const clampedValue = Math.max(props.min, Math.min(props.max, newValue));
    const newValues = [...currentValues];
    newValues[dragState.index] = clampedValue;
    
    if (newValues.length === 2) {
      if (dragState.index === 0 && newValues[0] > newValues[1]) {
        newValues[0] = newValues[1];
      } else if (dragState.index === 1 && newValues[1] < newValues[0]) {
        newValues[1] = newValues[0];
      }
    }
    
    internalValue.value = newValues.length === 1 ? newValues[0] : newValues;
    modelValue.value = internalValue.value;
  };

  const handleMouseUp = () => {
    dragState = null;
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
    document.removeEventListener('touchmove', handleTouchMove);
    document.removeEventListener('touchend', handleMouseUp);
  };

  const handleTouchMove = (touchEvent: TouchEvent) => {
    if (!dragState || touchEvent.touches.length === 0) return;
    const touch = touchEvent.touches[0];
    handleMouseMove({
      clientX: touch.clientX,
      clientY: touch.clientY,
    } as MouseEvent);
  };

  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
  document.addEventListener('touchmove', handleTouchMove, { passive: false });
  document.addEventListener('touchend', handleMouseUp);
};

const handleTrackClick = (e: MouseEvent) => {
  if (props.disabled || dragState) return;
  
  const trackElement = e.currentTarget as HTMLElement;
  const newValue = getValueFromPosition(
    props.orientation === 'horizontal' ? e.clientX : e.clientY,
    trackElement
  );
  
  const clampedValue = Math.max(props.min, Math.min(props.max, newValue));
  
  if (Array.isArray(internalValue.value)) {
    const newValues = [...internalValue.value];
    const closestIndex = newValues.reduce((closest, val, idx) => {
      return Math.abs(val - clampedValue) < Math.abs(newValues[closest] - clampedValue) 
        ? idx 
        : closest;
    }, 0);
    newValues[closestIndex] = clampedValue;
    if (newValues.length === 2) {
      if (closestIndex === 0 && newValues[0] > newValues[1]) {
        newValues[0] = newValues[1];
      } else if (closestIndex === 1 && newValues[1] < newValues[0]) {
        newValues[1] = newValues[0];
      }
    }
    internalValue.value = newValues;
  } else {
    internalValue.value = clampedValue;
  }
  
  modelValue.value = internalValue.value;
};

onUnmounted(() => {
  dragState = null;
});
</script>

<template>
  <div
    data-slot="slider"
    :class="rootClasses"
    :data-disabled="disabled"
    :data-orientation="orientation"
  >
    <div
      data-slot="slider-track"
      :class="trackClasses"
      :data-orientation="orientation"
      @click="handleTrackClick"
    >
      <div
        data-slot="slider-range"
        :class="rangeClasses"
        :style="rangeStyle"
        :data-orientation="orientation"
      />
    </div>
    <div
      v-for="(val, index) in values"
      :key="index"
      data-slot="slider-thumb"
      :class="thumbClasses"
      :style="thumbStyles[index]"
      :tabindex="disabled ? -1 : 0"
      role="slider"
      :aria-valuemin="min"
      :aria-valuemax="max"
      :aria-valuenow="val"
      :aria-disabled="disabled"
      @mousedown="(e) => handleMouseDown(e, index)"
      @touchstart="(e) => handleMouseDown(e.touches[0] as any, index)"
    />
  </div>
</template>
