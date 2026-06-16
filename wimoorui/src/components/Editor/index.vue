<template>
  <TinyMCEEditor
    :model-value="modelValue"
    :editor-key="editorKey"
    :config="editorConfig"
    @update:model-value="$emit('update:modelValue', $event)"
    @change="$emit('change', $event)"
    @init="$emit('init', $event)"
    @mce-new-document="$emit('mceNewDocument')"
  />
</template>

<script setup>
import { computed } from 'vue';
import TinyMCEEditor from '@/components/TinyMCE/TinyMCEEditor.vue';

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: Number, default: 400 },
  minHeight: { type: Number, default: null },
  readOnly: { type: Boolean, default: false },
  fileSize: { type: Number, default: 5 },
  type: { type: String, default: 'url' }
});

defineEmits(['update:modelValue', 'change', 'init', 'mceNewDocument']);

const editorKey = `legacy-editor-${Math.random().toString(36).slice(2)}`;

const editorConfig = computed(() => {
  const config = {
    height: props.height || 400,
    readonly: props.readOnly
  };
  if (props.minHeight) {
    config.min_height = props.minHeight;
  }
  return config;
});
</script>
