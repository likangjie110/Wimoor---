# ERP Purchase And Shipment Flow

```mermaid
flowchart TB
  material["Material and SKU master data"]
  plan["Purchase / replenishment plan"]
  purchase["Purchase form"]
  inbound["Warehouse inbound"]
  inventory["Inventory manager"]
  shipPlan["Shipment plan"]
  amazonShip["Amazon inbound shipment"]
  outbound["Warehouse outbound"]
  finance["Purchase finance / settlement"]
  external1688["1688 order tracking"]

  material --> plan
  plan --> purchase
  purchase --> external1688
  purchase --> inbound
  inbound --> inventory
  inventory --> shipPlan
  shipPlan --> amazonShip
  amazonShip --> outbound
  purchase --> finance
  outbound --> finance
```

Key backend families: `MaterialController`, `PlanController`, `PurchaseFormController`, `InventoryManagerController`, `WarehouseController`, `ShipPlanController`, `ShipFormController`, `AlibabaController`.

