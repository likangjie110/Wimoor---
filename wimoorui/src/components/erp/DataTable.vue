<template>
	<div class="erp-data-table" :class="tableClass">
		<div v-if="slots.toolbar || slots.toolbarExtra || slots.summary" class="erp-data-table__toolbar">
			<div class="erp-data-table__toolbar-main">
				<slot name="toolbar"></slot>
			</div>
			<div class="erp-data-table__toolbar-side">
				<slot name="summary"></slot>
				<slot name="toolbarExtra"></slot>
			</div>
		</div>
		<div ref="wrapRef" class="erp-data-table__wrap">
			<GlobalTable
				ref="tableRef"
				v-bind="forwardedAttrs"
				:height="resolvedHeight"
				@loadTable="handleLoadTable"
				@selectionChange="handleSelectionChange"
				@selection-change="handleSelectionChangeLegacy"
				@row-click="handleRowClick"
				@expandChange="handleExpandChange"
				@currentChange="handleCurrentChangeEvent"
			>
				<template v-if="slots.field" #field="slotProps">
					<slot name="field" v-bind="slotProps"></slot>
				</template>
			</GlobalTable>
		</div>
	</div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, useAttrs, useSlots } from "vue";

import GlobalTable from "@/components/Table/GlobalTable/index.vue";

defineOptions({
	name: "DataTable",
	inheritAttrs: false,
});

const attrs = useAttrs();
const slots = useSlots();

const props = defineProps({
	card: {
		type: Boolean,
		default: false,
	},
	fill: {
		type: Boolean,
		default: true,
	},
	autoHeight: {
		type: Boolean,
		default: true,
	},
	minHeight: {
		type: Number,
		default: 320,
	},
});

const emit = defineEmits(["loadTable", "selectionChange", "selection-change", "row-click", "expandChange", "currentChange"]);

const wrapRef = ref();
const tableRef = ref();
const observedHeight = ref(0);
let resizeObserver = null;

const explicitHeight = computed(() => {
	return attrs.height;
});

const resolvedHeight = computed(() => {
	if (explicitHeight.value !== undefined) {
		return explicitHeight.value;
	}

	if (props.autoHeight && observedHeight.value > 0) {
		return observedHeight.value;
	}

	return undefined;
});

const forwardedAttrs = computed(() => {
	const nextAttrs = { ...attrs };
	delete nextAttrs.height;
	return nextAttrs;
});

const tableClass = computed(() => {
	return {
		"is-card": props.card,
		"is-fill": props.fill,
	};
});

function updateHeight(entry) {
	const nextHeight = Math.max(props.minHeight, Math.floor(entry.contentRect.height));
	if (nextHeight !== observedHeight.value) {
		observedHeight.value = nextHeight;
	}
}

function callTableMethod(methodName, ...args) {
	const instance = tableRef.value;
	if (instance && typeof instance[methodName] === "function") {
		return instance[methodName](...args);
	}
	return undefined;
}

function loadTable(...args) {
	return callTableMethod("loadTable", ...args);
}

function refreshTable(...args) {
	return callTableMethod("refreshTable", ...args);
}

function refreshField(...args) {
	return callTableMethod("refreshField", ...args);
}

function handleCurrentChange(...args) {
	return callTableMethod("handleCurrentChange", ...args);
}

function toggleRowExpansion(...args) {
	return callTableMethod("toggleRowExpansion", ...args);
}

function toggleRowSelection(...args) {
	return callTableMethod("toggleRowSelection", ...args);
}

function setCurrentRow(...args) {
	return callTableMethod("setCurrentRow", ...args);
}

function getSelectionRows(...args) {
	return callTableMethod("getSelectionRows", ...args);
}

function doLayout(...args) {
	return callTableMethod("doLayout", ...args);
}

function changeSize(...args) {
	return callTableMethod("changeSize", ...args);
}

function tableSort(...args) {
	return callTableMethod("tableSort", ...args);
}

function setScrollLeft(...args) {
	return callTableMethod("setScrollLeft", ...args);
}

function setScrollTop(...args) {
	return callTableMethod("setScrollTop", ...args);
}

function handleLoadTable(...args) {
	emit("loadTable", ...args);
}

function handleSelectionChange(...args) {
	emit("selectionChange", ...args);
}

function handleSelectionChangeLegacy(...args) {
	emit("selection-change", ...args);
}

function handleRowClick(...args) {
	emit("row-click", ...args);
}

function handleExpandChange(...args) {
	emit("expandChange", ...args);
}

function handleCurrentChangeEvent(...args) {
	emit("currentChange", ...args);
}

onMounted(() => {
	if (!props.autoHeight || explicitHeight.value !== undefined || !wrapRef.value || typeof ResizeObserver === "undefined") {
		return;
	}

	resizeObserver = new ResizeObserver((entries) => {
		const [entry] = entries;
		if (entry) {
			updateHeight(entry);
		}
	});

	resizeObserver.observe(wrapRef.value);
});

onBeforeUnmount(() => {
	if (resizeObserver) {
		resizeObserver.disconnect();
	}
});

defineExpose({
	loadTable,
	refreshTable,
	refreshField,
	handleCurrentChange,
	toggleRowExpansion,
	toggleRowSelection,
	setCurrentRow,
	getSelectionRows,
	doLayout,
	changeSize,
	tableSort,
	setScrollLeft,
	setScrollTop,
	tableRef,
});
</script>

<style scoped>
.erp-data-table {
	display: flex;
	flex-direction: column;
	gap: 16px;
	min-height: 0;
}

.erp-data-table.is-card {
	padding: 16px;
	background-color: #fff;
	border-radius: 4px;
	box-shadow: 0 2px 6px 0 rgba(0, 0, 0, 0.1);
}

.dark .erp-data-table.is-card {
	background-color: #000;
}

.erp-data-table.is-fill {
	flex: 1;
}

.erp-data-table__toolbar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	flex-wrap: wrap;
}

.erp-data-table__toolbar-main,
.erp-data-table__toolbar-side {
	display: flex;
	align-items: center;
	gap: 12px;
	flex-wrap: wrap;
}

.erp-data-table__toolbar-side {
	margin-left: auto;
}

.erp-data-table__wrap {
	display: flex;
	flex: 1;
	min-height: 0;
}

.erp-data-table__wrap :deep(.el-pagination) {
	margin-top: auto;
}
</style>
