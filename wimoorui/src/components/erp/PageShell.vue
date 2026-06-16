<template>
	<section class="erp-page-shell" :class="shellClass">
		<header v-if="hasHeader" class="erp-page-shell__header">
			<div v-if="hasIdentity" class="erp-page-shell__identity">
				<div class="erp-page-shell__identity-main">
					<div v-if="code || slots.eyebrow" class="erp-page-shell__eyebrow">
						<slot name="eyebrow">{{ code }}</slot>
					</div>
					<div class="erp-page-shell__title-row">
						<h3 class="erp-page-shell__title">
							<slot name="title">{{ title }}</slot>
						</h3>
						<div v-if="slots.titleSuffix" class="erp-page-shell__title-suffix">
							<slot name="titleSuffix"></slot>
						</div>
					</div>
					<p v-if="subtitle || slots.subtitle" class="erp-page-shell__subtitle">
						<slot name="subtitle">{{ subtitle }}</slot>
					</p>
				</div>
				<div v-if="slots.headerExtra" class="erp-page-shell__header-extra">
					<slot name="headerExtra"></slot>
				</div>
			</div>
			<div v-if="slots.filters" class="erp-page-shell__filters">
				<slot name="filters"></slot>
			</div>
			<div v-if="slots.actions || slots.summary" class="erp-page-shell__toolbar">
				<div class="erp-page-shell__actions">
					<slot name="actions"></slot>
				</div>
				<div class="erp-page-shell__summary">
					<slot name="summary"></slot>
				</div>
			</div>
		</header>
		<div class="erp-page-shell__body" :style="bodyStyle">
			<slot></slot>
		</div>
		<footer v-if="slots.footer" class="erp-page-shell__footer">
			<slot name="footer"></slot>
		</footer>
	</section>
</template>

<script setup>
import { computed, useSlots } from "vue";

defineOptions({
	name: "PageShell",
});

const slots = useSlots();

const props = defineProps({
	title: {
		type: String,
		default: "",
	},
	subtitle: {
		type: String,
		default: "",
	},
	code: {
		type: String,
		default: "",
	},
	card: {
		type: Boolean,
		default: true,
	},
	fill: {
		type: Boolean,
		default: false,
	},
	bodyPadding: {
		type: String,
		default: "0",
	},
	contentGap: {
		type: [String, Number],
		default: 16,
	},
	headerBorder: {
		type: Boolean,
		default: false,
	},
});

const hasIdentity = computed(() => {
	return Boolean(props.title || props.subtitle || props.code || slots.title || slots.subtitle || slots.eyebrow || slots.headerExtra || slots.titleSuffix);
});

const hasHeader = computed(() => {
	return Boolean(hasIdentity.value || slots.filters || slots.actions || slots.summary);
});

const shellClass = computed(() => {
	return {
		"is-card": props.card,
		"is-fill": props.fill,
		"has-header-border": props.headerBorder,
	};
});

const bodyStyle = computed(() => {
	const gap = typeof props.contentGap === "number" ? `${props.contentGap}px` : props.contentGap;

	return {
		padding: props.bodyPadding,
		gap,
	};
});
</script>

<style scoped>
.erp-page-shell {
	display: flex;
	flex-direction: column;
	gap: 16px;
	min-height: 0;
}

.erp-page-shell.is-card {
	margin: 16px;
	padding: 16px;
	background-color: #fff;
	border-radius: 4px;
	box-shadow: 0 2px 6px 0 rgba(0, 0, 0, 0.1);
}

.dark .erp-page-shell.is-card {
	background-color: #000;
}

.erp-page-shell.is-fill {
	flex: 1;
	height: 100%;
}

.erp-page-shell__header {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.erp-page-shell.has-header-border .erp-page-shell__header {
	padding-bottom: 16px;
	border-bottom: 1px solid var(--el-border-color-light);
}

.erp-page-shell__identity {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16px;
	flex-wrap: wrap;
}

.erp-page-shell__identity-main {
	display: flex;
	flex-direction: column;
	gap: 4px;
	min-width: 0;
}

.erp-page-shell__eyebrow {
	font-size: var(--el-font-size-extra-small);
	color: var(--el-text-color-placeholder);
	text-transform: uppercase;
	letter-spacing: 0.08em;
}

.erp-page-shell__title-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 8px;
}

.erp-page-shell__title {
	margin: 0;
	font-size: var(--el-font-size-medium);
	line-height: 1.4;
	color: var(--el-text-color-primary);
}

.erp-page-shell__subtitle {
	margin: 0;
	font-size: var(--el-font-size-small);
	color: var(--el-text-color-secondary);
}

.erp-page-shell__header-extra,
.erp-page-shell__summary {
	margin-left: auto;
}

.erp-page-shell__toolbar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	flex-wrap: wrap;
}

.erp-page-shell__actions {
	display: flex;
	align-items: center;
	gap: 12px;
	flex-wrap: wrap;
}

.erp-page-shell__body {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-height: 0;
}

.erp-page-shell__footer {
	padding-top: 8px;
	border-top: 1px solid var(--el-border-color-lighter);
}

@media screen and (max-width: 1440px) {
	.erp-page-shell.is-card {
		margin: 12px;
	}
}
</style>
