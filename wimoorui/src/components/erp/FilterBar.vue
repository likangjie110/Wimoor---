<template>
	<div class="erp-filter-bar">
		<div class="erp-filter-bar__row">
			<div class="erp-filter-bar__main">
				<slot></slot>
			</div>
			<div v-if="hasTools" class="erp-filter-bar__tools">
				<slot name="trailing"></slot>
				<el-popover
					v-if="slots.advanced"
					v-model:visible="advancedVisible"
					:width="advancedWidth"
					:teleported="teleported"
					trigger="click"
				>
					<template #reference>
						<el-button class="ic-btn" :title="advancedButtonText">
							<filtericon></filtericon>
							<span>{{ advancedButtonText }}</span>
						</el-button>
					</template>
					<div class="erp-filter-bar__advanced">
						<slot name="advanced"></slot>
					</div>
				</el-popover>
				<el-button v-if="showResetButton" @click="emit('reset')">{{ resetButtonText }}</el-button>
				<el-button v-if="showSearchButton" type="primary" @click="emit('search')">{{ searchButtonText }}</el-button>
			</div>
		</div>
		<div v-if="slots.actions || slots.summary" class="erp-filter-bar__row erp-filter-bar__row--secondary">
			<div class="erp-filter-bar__actions">
				<slot name="actions"></slot>
			</div>
			<div class="erp-filter-bar__summary">
				<slot name="summary"></slot>
			</div>
		</div>
	</div>
</template>

<script setup>
import { computed, ref, useSlots, watch } from "vue";

import filtericon from "@/components/icon/filtericon.vue";

defineOptions({
	name: "FilterBar",
});

const slots = useSlots();

const props = defineProps({
	showSearchButton: {
		type: Boolean,
		default: false,
	},
	showResetButton: {
		type: Boolean,
		default: false,
	},
	searchButtonText: {
		type: String,
		default: "查询",
	},
	resetButtonText: {
		type: String,
		default: "重置",
	},
	advancedButtonText: {
		type: String,
		default: "高级筛选",
	},
	advancedWidth: {
		type: [String, Number],
		default: 420,
	},
	advancedVisible: {
		type: Boolean,
		default: false,
	},
	teleported: {
		type: Boolean,
		default: true,
	},
});

const emit = defineEmits(["search", "reset", "update:advancedVisible"]);

const advancedVisible = ref(props.advancedVisible);

watch(
	() => props.advancedVisible,
	(value) => {
		advancedVisible.value = value;
	}
);

watch(advancedVisible, (value) => {
	emit("update:advancedVisible", value);
});

const hasTools = computed(() => {
	return Boolean(slots.trailing || slots.advanced || props.showResetButton || props.showSearchButton);
});
</script>

<style scoped>
.erp-filter-bar {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.erp-filter-bar__row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16px;
	flex-wrap: wrap;
}

.erp-filter-bar__main,
.erp-filter-bar__actions {
	display: flex;
	align-items: center;
	gap: 12px;
	flex-wrap: wrap;
	flex: 1;
	min-width: 0;
}

.erp-filter-bar__tools,
.erp-filter-bar__summary {
	display: flex;
	align-items: center;
	gap: 12px;
	flex-wrap: wrap;
	margin-left: auto;
}

.erp-filter-bar__row--secondary {
	align-items: center;
}

.erp-filter-bar__advanced {
	display: flex;
	flex-direction: column;
	gap: 16px;
}
</style>
