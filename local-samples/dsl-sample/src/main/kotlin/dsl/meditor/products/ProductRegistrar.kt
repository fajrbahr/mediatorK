package dsl.meditor.products

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar

class ProductRegistrar(
    private val repo: PriceRepo,
    private val pushService: PushService,
    private val inAppService: InAppService,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +getPriceFeature(repo, pushService, inAppService)
        }
    }
}
