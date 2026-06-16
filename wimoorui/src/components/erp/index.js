import DataTable from "./DataTable.vue";
import FilterBar from "./FilterBar.vue";
import PageShell from "./PageShell.vue";
import StepPage from "./StepPage.vue";

const components = [
	["PageShell", PageShell],
	["FilterBar", FilterBar],
	["DataTable", DataTable],
	["StepPage", StepPage],
];

export { PageShell, FilterBar, DataTable, StepPage };

export default {
	install(app) {
		components.forEach(([name, component]) => {
			app.component(name, component);
		});
	},
};
