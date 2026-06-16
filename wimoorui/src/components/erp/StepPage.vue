<template>
	<PageShell
		:title="title"
		:subtitle="subtitle"
		:code="code"
		:card="card"
		:fill="fill"
		:content-gap="0"
		class="erp-step-page"
	>
		<template v-if="slots.headerExtra" #headerExtra>
			<slot name="headerExtra"></slot>
		</template>
		<div class="erp-step-page__content">
			<div v-if="steps.length > 0" class="erp-step-page__steps">
				<el-steps :active="activeIndex" :simple="simple" align-center>
					<el-step v-for="(step, index) in steps" :key="step[stepKey] ?? index">
						<template #title>
							<span
								class="erp-step-page__step-title"
								:class="{ 'is-clickable': clickable && !step.disabled }"
								@click="handleStepClick(step, index)"
							>
								{{ step[stepTitle] ?? `步骤 ${index + 1}` }}
							</span>
						</template>
						<template v-if="showStepDescription && step[stepDescription]" #description>
							{{ step[stepDescription] }}
						</template>
					</el-step>
				</el-steps>
			</div>
			<div v-if="currentHint || slots.hint" class="erp-step-page__hint">
				<slot name="hint">{{ currentHint }}</slot>
			</div>
			<div class="erp-step-page__body">
				<slot></slot>
			</div>
		</div>
		<template v-if="slots.footer" #footer>
			<slot name="footer"></slot>
		</template>
	</PageShell>
</template>

<script setup>
import { computed, useSlots } from "vue";

import PageShell from "./PageShell.vue";

defineOptions({
	name: "StepPage",
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
	steps: {
		type: Array,
		default: () => [],
	},
	activeStep: {
		type: [String, Number],
		default: 0,
	},
	card: {
		type: Boolean,
		default: true,
	},
	fill: {
		type: Boolean,
		default: false,
	},
	clickable: {
		type: Boolean,
		default: true,
	},
	simple: {
		type: Boolean,
		default: false,
	},
	showStepDescription: {
		type: Boolean,
		default: false,
	},
	stepKey: {
		type: String,
		default: "key",
	},
	stepTitle: {
		type: String,
		default: "title",
	},
	stepDescription: {
		type: String,
		default: "description",
	},
});

const emit = defineEmits(["step-change"]);

const activeIndex = computed(() => {
	if (typeof props.activeStep === "number") {
		return props.activeStep;
	}

	const foundIndex = props.steps.findIndex((step) => {
		return step[props.stepKey] === props.activeStep;
	});

	return foundIndex >= 0 ? foundIndex : 0;
});

const currentStep = computed(() => {
	return props.steps[activeIndex.value] || null;
});

const currentHint = computed(() => {
	if (!currentStep.value) {
		return "";
	}

	return currentStep.value[props.stepDescription] || "";
});

function handleStepClick(step, index) {
	if (!props.clickable || step.disabled) {
		return;
	}

	emit("step-change", step, index);
}
</script>

<style scoped>
.erp-step-page {
	min-height: 0;
}

.erp-step-page__content {
	display: flex;
	flex: 1;
	flex-direction: column;
	gap: 16px;
	min-height: 0;
}

.erp-step-page__steps {
	padding-bottom: 8px;
	border-bottom: 1px solid var(--el-border-color-lighter);
}

.erp-step-page__step-title {
	display: inline-flex;
	align-items: center;
	line-height: 1.4;
}

.erp-step-page__step-title.is-clickable {
	cursor: pointer;
}

.erp-step-page__hint {
	padding: 12px 16px;
	font-size: var(--el-font-size-small);
	color: var(--el-text-color-secondary);
	background-color: var(--el-color-primary-light-9);
	border-radius: 4px;
}

.dark .erp-step-page__hint {
	background-color: rgba(255, 115, 21, 0.12);
}

.erp-step-page__body {
	display: flex;
	flex: 1;
	flex-direction: column;
	gap: 16px;
	min-height: 0;
}

.erp-step-page :deep(.el-step__title) {
	font-size: var(--el-font-size-base);
}
</style>
