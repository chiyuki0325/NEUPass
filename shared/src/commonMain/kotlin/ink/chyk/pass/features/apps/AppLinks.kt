package ink.chyk.pass.features.apps

import com.tencent.tmm.kmmresource.resource.ImageResource
import com.tencent.tmm.kmmresource.resource.StringResource
import ink.chyk.pass.api.CampusApp
import ink.chyk.pass.resources.MR

data class AppLinks(
  val name: StringResource,
  val icon: ImageResource,
  val app: CampusApp,
) {
  companion object {
    val links = listOf(
      AppLinks(
        MR.strings.xg,
        MR.images.ic_fluent_calendar_template_24_regular,
        CampusApp.XG
      ),
      AppLinks(
        MR.strings.payment_query,
        MR.images.ic_fluent_wallet_credit_card_24_regular,
        CampusApp.PAYMENT_QUERY
      ),
      AppLinks(
        MR.strings.blackboard_platform,
        MR.images.ic_fluent_whiteboard_24_regular,
        CampusApp.BLACKBOARD
      ),
      AppLinks(
        MR.strings.so_platform,
        MR.images.ic_fluent_search_sparkle_24_regular,
        CampusApp.SEARCH
      ),
      AppLinks(
        MR.strings.webvpn,
        MR.images.ic_fluent_router_24_regular,
        CampusApp.WEBVPN
      ),
      AppLinks(
        MR.strings.library,
        MR.images.ic_fluent_library_24_regular,
        CampusApp.LIBRARY
      ),
      AppLinks(
        MR.strings.bus_booking,
        MR.images.ic_fluent_vehicle_bus_24_regular,
        CampusApp.BUS_BOOKING
      ),
      AppLinks(
        MR.strings.place_booking,
        MR.images.ic_fluent_conference_room_24_regular,
        CampusApp.PLACE_BOOKING
      )
    )
  }
}
