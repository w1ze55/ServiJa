<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import {
  PopoverRoot,
  PopoverTrigger,
  PopoverPortal,
  PopoverContent,
  CalendarRoot,
  CalendarHeader,
  CalendarHeading,
  CalendarPrev,
  CalendarNext,
  CalendarGrid,
  CalendarGridHead,
  CalendarGridBody,
  CalendarGridRow,
  CalendarHeadCell,
  CalendarCell,
  CalendarCellTrigger,
} from 'reka-ui';
import { parseDate, type DateValue } from '@internationalized/date';
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  modelValue?: string; // YYYY-MM-DD
  min?: string; // YYYY-MM-DD
  max?: string; // YYYY-MM-DD
  placeholder?: string;
  disabled?: boolean;
  id?: string;
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: 'dd/mm/aaaa',
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const open = ref(false);
const focused = ref(false);
const displayValue = ref('');

// YYYY-MM-DD -> DD/MM/YYYY
const isoToBr = (iso?: string) => {
  if (!iso) return '';
  const [year, month, day] = iso.split('-');
  if (!year || !month || !day) return '';
  return `${day}/${month}/${year}`;
};

// Mantém apenas dígitos e os formata progressivamente como DD/MM/YYYY
const formatDigits = (raw: string) => {
  const digits = raw.replace(/\D/g, '').slice(0, 8);
  const parts = [digits.slice(0, 2)];
  if (digits.length > 2) parts.push(digits.slice(2, 4));
  if (digits.length > 4) parts.push(digits.slice(4, 8));
  return parts.join('/');
};

// Valida e converte DD/MM/YYYY -> YYYY-MM-DD (string vazia se incompleto/inválido)
const brToIso = (value: string) => {
  const digits = value.replace(/\D/g, '');
  if (digits.length !== 8) return '';
  const day = Number(digits.slice(0, 2));
  const month = Number(digits.slice(2, 4));
  const year = Number(digits.slice(4, 8));
  const date = new Date(year, month - 1, day);
  const isRealDate =
    date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
  if (!isRealDate) return '';
  return `${digits.slice(4, 8)}-${digits.slice(2, 4)}-${digits.slice(0, 2)}`;
};

// Sincroniza o texto exibido quando o valor externo muda (sem atrapalhar a digitação)
watch(
  () => props.modelValue,
  (value) => {
    if (!focused.value) displayValue.value = isoToBr(value);
  },
  { immediate: true },
);

const onInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  displayValue.value = formatDigits(target.value);
  emit('update:modelValue', brToIso(displayValue.value));
};

const onBlur = () => {
  focused.value = false;
  displayValue.value = isoToBr(props.modelValue);
};

const toDateValue = (value?: string): DateValue | undefined => {
  if (!value) return undefined;
  try {
    return parseDate(value);
  } catch {
    return undefined;
  }
};

const selectedDate = computed<DateValue | undefined>({
  get: () => toDateValue(props.modelValue),
  set: (value) => emit('update:modelValue', value ? value.toString() : ''),
});

const minValue = computed(() => toDateValue(props.min));
const maxValue = computed(() => toDateValue(props.max));

const handleCalendarSelect = () => {
  open.value = false;
};

const containerClasses = computed(() =>
  cn(
    'flex h-10 w-full items-center gap-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900',
    props.disabled ? 'pointer-events-none cursor-not-allowed opacity-50' : '',
    props.class,
  ),
);
</script>

<template>
  <div :class="containerClasses">
    <input
      :id="id"
      type="text"
      inputmode="numeric"
      autocomplete="off"
      :value="displayValue"
      :placeholder="placeholder"
      :disabled="disabled"
      maxlength="10"
      class="min-w-0 flex-1 bg-transparent tabular-nums outline-none placeholder:text-slate-400 disabled:cursor-not-allowed"
      @input="onInput"
      @focus="focused = true"
      @blur="onBlur"
    />

    <PopoverRoot v-model:open="open">
      <PopoverTrigger
        type="button"
        :disabled="disabled"
        aria-label="Abrir calendário"
        class="flex h-6 w-6 shrink-0 items-center justify-center rounded text-slate-400 outline-none transition-colors hover:text-slate-600"
      >
        <CalendarIcon class="h-4 w-4" />
      </PopoverTrigger>

      <PopoverPortal>
        <PopoverContent
          :side-offset="6"
          align="end"
          class="z-50 rounded-lg border border-slate-200 bg-white p-3 text-slate-900 shadow-lg outline-none"
        >
          <CalendarRoot
            v-slot="{ weekDays, grid }"
            v-model="selectedDate"
            :min-value="minValue"
            :max-value="maxValue"
            locale="pt-BR"
            weekday-format="narrow"
            class="select-none"
            @update:model-value="handleCalendarSelect"
          >
            <CalendarHeader class="relative flex items-center justify-center pt-1">
              <CalendarPrev
                class="absolute left-1 inline-flex h-7 w-7 items-center justify-center rounded-md border border-slate-200 bg-transparent text-slate-500 transition-colors hover:bg-slate-50"
              >
                <ChevronLeft class="h-4 w-4" />
              </CalendarPrev>
              <CalendarHeading class="text-sm font-medium capitalize text-slate-700" />
              <CalendarNext
                class="absolute right-1 inline-flex h-7 w-7 items-center justify-center rounded-md border border-slate-200 bg-transparent text-slate-500 transition-colors hover:bg-slate-50"
              >
                <ChevronRight class="h-4 w-4" />
              </CalendarNext>
            </CalendarHeader>

            <div class="flex flex-col space-y-4 pt-4 sm:flex-row sm:space-x-4 sm:space-y-0">
              <CalendarGrid
                v-for="month in grid"
                :key="month.value.toString()"
                class="w-full border-collapse space-y-1"
              >
                <CalendarGridHead>
                  <CalendarGridRow class="flex">
                    <CalendarHeadCell
                      v-for="day in weekDays"
                      :key="day"
                      class="w-8 rounded-md text-[0.8rem] font-normal capitalize text-slate-400"
                    >
                      {{ day }}
                    </CalendarHeadCell>
                  </CalendarGridRow>
                </CalendarGridHead>
                <CalendarGridBody class="grid">
                  <CalendarGridRow
                    v-for="(weekDates, index) in month.rows"
                    :key="`weekDate-${index}`"
                    class="mt-2 flex w-full"
                  >
                    <CalendarCell
                      v-for="weekDate in weekDates"
                      :key="weekDate.toString()"
                      :date="weekDate"
                      class="relative p-0 text-center text-sm"
                    >
                      <CalendarCellTrigger
                        :day="weekDate"
                        :month="month.value"
                        class="inline-flex h-8 w-8 items-center justify-center rounded-md p-0 text-sm font-normal text-slate-700 outline-none transition-colors hover:bg-slate-100 data-[today]:bg-slate-100 data-[today]:font-medium data-[outside-view]:text-slate-300 data-[disabled]:text-slate-300 data-[disabled]:opacity-50 data-[unavailable]:pointer-events-none data-[unavailable]:text-slate-300 data-[unavailable]:line-through data-[selected]:bg-blue-600 data-[selected]:text-white data-[selected]:hover:bg-blue-600"
                      />
                    </CalendarCell>
                  </CalendarGridRow>
                </CalendarGridBody>
              </CalendarGrid>
            </div>
          </CalendarRoot>
        </PopoverContent>
      </PopoverPortal>
    </PopoverRoot>
  </div>
</template>
