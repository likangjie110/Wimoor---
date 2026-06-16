<template>
	<div class="erp-foundation-demo">
		<PageShell
			title="ERP 基础组件预览"
			subtitle="第一版统一了查询页骨架和步骤页骨架，保留高密度桌面 ERP 操作风格。"
			code="UI FOUNDATION"
		>
			<template #headerExtra>
				<el-tag effect="plain" type="warning">跨境 ERP 原型</el-tag>
			</template>
			<template #filters>
				<FilterBar
					show-search-button
					show-reset-button
					:advanced-visible="advancedVisible"
					@search="applyFilters"
					@reset="resetFilters"
					@update:advancedVisible="advancedVisible = $event"
				>
					<el-select v-model="filters.warehouse" clearable placeholder="仓库" style="width: 140px">
						<el-option label="全部仓库" value=""></el-option>
						<el-option label="深圳本地仓" value="深圳本地仓"></el-option>
						<el-option label="洛杉矶海外仓" value="洛杉矶海外仓"></el-option>
						<el-option label="德国海外仓" value="德国海外仓"></el-option>
					</el-select>
					<el-select v-model="filters.status" clearable placeholder="状态" style="width: 140px">
						<el-option label="全部状态" value=""></el-option>
						<el-option label="正常" value="normal"></el-option>
						<el-option label="预警" value="warning"></el-option>
						<el-option label="缺货" value="danger"></el-option>
					</el-select>
					<el-input v-model="filters.keyword" clearable placeholder="搜索 SKU / 名称" style="width: 240px"></el-input>
					<template #advanced>
						<el-form label-width="84px" label-position="left">
							<el-form-item label="负责人">
								<el-select v-model="filters.owner" clearable placeholder="请选择负责人">
									<el-option label="采购A" value="采购A"></el-option>
									<el-option label="采购B" value="采购B"></el-option>
									<el-option label="运营C" value="运营C"></el-option>
								</el-select>
							</el-form-item>
							<el-form-item label="仅显示预警">
								<el-switch v-model="filters.onlyWarning"></el-switch>
							</el-form-item>
						</el-form>
					</template>
					<template #actions>
						<el-button type="primary">创建计划</el-button>
						<el-button>导出结果</el-button>
						<el-button>批量发货</el-button>
					</template>
					<template #summary>
						<el-space class="font-small">
							<span>结果 <span class="text-orange font-bold">{{ tableData.total }}</span> 个SKU</span>
							<el-divider direction="vertical" />
							<span>已选 <span class="text-orange font-bold">{{ selectedRows.length }}</span> 个SKU</span>
						</el-space>
					</template>
				</FilterBar>
			</template>
			<DataTable :table-data="tableData" nopage @selectionChange="selectedRows = $event">
				<template #toolbar>
					<div class="font-extraSmall">`DataTable` 透传 `GlobalTable` 能力，页面只负责列定义和数据处理。</div>
				</template>
				<template #field>
					<el-table-column type="selection" width="42" fixed />
					<el-table-column prop="sku" label="名称 / SKU" min-width="240" show-overflow-tooltip>
						<template #default="scope">
							<div class="font-bold">{{ scope.row.sku }}</div>
							<div class="font-extraSmall text-omit-1">{{ scope.row.name }}</div>
						</template>
					</el-table-column>
					<el-table-column prop="warehouse" label="仓库" width="160"></el-table-column>
					<el-table-column prop="owner" label="负责人" width="120"></el-table-column>
					<el-table-column prop="available" label="可用库存" width="110"></el-table-column>
					<el-table-column prop="days" label="可售天数" width="120">
						<template #default="scope">
							<span :class="scope.row.days < 14 ? 'text-danger' : 'text-success'">{{ scope.row.days }} 天</span>
						</template>
					</el-table-column>
					<el-table-column prop="planQty" label="建议补货" width="120"></el-table-column>
					<el-table-column prop="status" label="状态" width="110">
						<template #default="scope">
							<el-tag :type="statusMap[scope.row.status].type" effect="plain">{{ statusMap[scope.row.status].label }}</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="操作" fixed="right" width="140">
						<template #default>
							<el-space>
								<el-button type="primary" link>详情</el-button>
								<el-button type="primary" link>编辑</el-button>
							</el-space>
						</template>
					</el-table-column>
				</template>
			</DataTable>
		</PageShell>

		<StepPage
			title="发货处理"
			subtitle="示例展示 `StepPage` 如何承载跨境 ERP 的单据上下文和步骤切换。"
			code="SHIP-20260418-001"
			:steps="steps"
			:active-step="activeStep"
			@step-change="handleStepChange"
		>
			<template #headerExtra>
				<el-space>
					<el-tag effect="plain">亚马逊美国站</el-tag>
					<el-tag type="success" effect="plain">FBA</el-tag>
				</el-space>
			</template>
			<div class="erp-foundation-demo__step-grid">
				<div class="pag-radius-bor">
					<h4>{{ steps[activeStep].title }}</h4>
					<p class="font-extraSmall m-t-8">{{ steps[activeStep].description }}</p>
					<el-divider></el-divider>
					<el-row :gutter="16">
						<el-col :span="8">
							<div class="erp-foundation-demo__metric">
								<div class="font-extraSmall">SKU 数量</div>
								<div class="erp-foundation-demo__metric-value">32</div>
							</div>
						</el-col>
						<el-col :span="8">
							<div class="erp-foundation-demo__metric">
								<div class="font-extraSmall">计划箱数</div>
								<div class="erp-foundation-demo__metric-value">18</div>
							</div>
						</el-col>
						<el-col :span="8">
							<div class="erp-foundation-demo__metric">
								<div class="font-extraSmall">预估重量</div>
								<div class="erp-foundation-demo__metric-value">286kg</div>
							</div>
						</el-col>
					</el-row>
				</div>
				<div class="pag-radius-bor">
					<h4>当前步骤说明</h4>
					<ul class="erp-foundation-demo__list">
						<li>步骤头统一展示单据编码、标题和当前业务上下文。</li>
						<li>步骤体允许继续嵌套表格、表单、汇总卡和弹窗操作。</li>
						<li>步骤切换默认可点击，也可以按业务规则限制跳转。</li>
					</ul>
				</div>
			</div>
			<template #footer>
				<div class="flex-center-between">
					<el-button :disabled="activeStep === 0" @click="activeStep = Math.max(activeStep - 1, 0)">上一步</el-button>
					<el-button type="primary" :disabled="activeStep === steps.length - 1" @click="activeStep = Math.min(activeStep + 1, steps.length - 1)">
						下一步
					</el-button>
				</div>
			</template>
		</StepPage>
	</div>
</template>

<script setup>
import { reactive, ref } from "vue";

defineOptions({
	name: "ErpFoundationDemo",
});

const statusMap = {
	normal: {
		label: "正常",
		type: "success",
	},
	warning: {
		label: "预警",
		type: "warning",
	},
	danger: {
		label: "缺货",
		type: "danger",
	},
};

const rawRecords = [
	{ sku: "WM-US-001", name: "便携收纳盒", warehouse: "深圳本地仓", owner: "采购A", available: 680, days: 48, planQty: 0, status: "normal" },
	{ sku: "WM-US-002", name: "折叠桌面支架", warehouse: "洛杉矶海外仓", owner: "运营C", available: 126, days: 12, planQty: 340, status: "warning" },
	{ sku: "WM-EU-007", name: "多功能厨房挂架", warehouse: "德国海外仓", owner: "采购B", available: 32, days: 6, planQty: 520, status: "danger" },
	{ sku: "WM-US-011", name: "磁吸收纳板", warehouse: "洛杉矶海外仓", owner: "采购A", available: 260, days: 21, planQty: 120, status: "warning" },
	{ sku: "WM-EU-012", name: "便携旅行分装瓶", warehouse: "深圳本地仓", owner: "采购B", available: 920, days: 67, planQty: 0, status: "normal" },
];

const filters = reactive({
	warehouse: "",
	status: "",
	keyword: "",
	owner: "",
	onlyWarning: false,
});

const advancedVisible = ref(false);
const selectedRows = ref([]);
const tableData = reactive({
	records: [],
	total: 0,
});

const steps = [
	{ key: "pick", title: "配货", description: "确认要发货的 SKU、数量和货件归属。" },
	{ key: "box", title: "装箱", description: "生成箱唛、校验装箱体积和预估重量。" },
	{ key: "config", title: "配置", description: "确认仓库、配送方式和承运信息。" },
	{ key: "ship", title: "发货", description: "提交承运信息并生成出库记录。" },
];

const activeStep = ref(0);

function applyFilters() {
	const records = rawRecords.filter((item) => {
		const matchWarehouse = !filters.warehouse || item.warehouse === filters.warehouse;
		const matchStatus = !filters.status || item.status === filters.status;
		const matchKeyword = !filters.keyword || item.sku.toLowerCase().includes(filters.keyword.toLowerCase()) || item.name.includes(filters.keyword);
		const matchOwner = !filters.owner || item.owner === filters.owner;
		const matchOnlyWarning = !filters.onlyWarning || item.status !== "normal";

		return matchWarehouse && matchStatus && matchKeyword && matchOwner && matchOnlyWarning;
	});

	tableData.records = records;
	tableData.total = records.length;
	selectedRows.value = [];
}

function resetFilters() {
	filters.warehouse = "";
	filters.status = "";
	filters.keyword = "";
	filters.owner = "";
	filters.onlyWarning = false;
	advancedVisible.value = false;
	applyFilters();
}

function handleStepChange(step, index) {
	activeStep.value = index;
}

applyFilters();
</script>

<style scoped>
.erp-foundation-demo {
	display: flex;
	flex-direction: column;
	gap: 16px;
	padding-bottom: 16px;
}

.erp-foundation-demo__step-grid {
	display: grid;
	grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr);
	gap: 16px;
}

.erp-foundation-demo__metric {
	padding: 12px 16px;
	background: var(--el-fill-color-light);
	border-radius: 4px;
}

.erp-foundation-demo__metric-value {
	margin-top: 8px;
	font-size: 22px;
	font-weight: 700;
	color: var(--el-text-color-primary);
}

.erp-foundation-demo__list {
	margin: 0;
	padding-left: 16px;
	color: var(--el-text-color-regular);
	line-height: 1.8;
}

@media screen and (max-width: 1440px) {
	.erp-foundation-demo__step-grid {
		grid-template-columns: 1fr;
	}
}
</style>
