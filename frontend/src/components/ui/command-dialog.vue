<script setup lang="ts">
import Command from './command.vue';
import Dialog from './dialog.vue';
import DialogContent from './dialog-content.vue';
import DialogDescription from './dialog-description.vue';
import DialogHeader from './dialog-header.vue';
import DialogTitle from './dialog-title.vue';

interface Props {
  open?: boolean;
  title?: string;
  description?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Command Palette',
  description: 'Search for a command to run...',
});
</script>

<template>
  <Dialog v-bind="$attrs">
    <DialogHeader class="sr-only">
      <DialogTitle>{{ props.title }}</DialogTitle>
      <DialogDescription>{{ props.description }}</DialogDescription>
    </DialogHeader>
    <DialogContent class="overflow-hidden p-0">
      <Command class="[&_[cmdk-group-heading]]:text-muted-foreground **:data-[slot=command-input-wrapper]:h-12 [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:font-medium [&_[cmdk-group]]:px-2 [&_[cmdk-group]:not([hidden])_~[cmdk-group]]:pt-0 [&_[cmdk-input-wrapper]_svg]:h-5 [&_[cmdk-input-wrapper]_svg]:w-5 [&_[cmdk-input]]:h-12 [&_[cmdk-item]]:px-2 [&_[cmdk-item]]:py-3 [&_[cmdk-item]_svg]:h-5 [&_[cmdk-item]_svg]:w-5">
        <slot />
      </Command>
    </DialogContent>
  </Dialog>
</template>

