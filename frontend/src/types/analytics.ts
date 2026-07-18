export interface TopSellingProduct {
  productId: string
  name: string
  unitsSold: number
}

export interface RevenuePoint {
  date: string
  amount: number
}

export interface ProductionPoint {
  date: string
  litres: number
}

export interface Dashboard {
  todayMilkProductionLitres: number
  activeAnimals: number
  activeSubscriptions: number
  monthRevenue: number
  totalOrders: number
  avgOrderValue: number
  inventoryValueTotal: number
  productsExpiringSoon: number
  topSellingProducts: TopSellingProduct[]
  revenueTrend: RevenuePoint[]
  productionTrend: ProductionPoint[]
}
