package me.masi.services.processors

import me.masi.dto.RankedLand
import me.masi.dto.RankedLandsEpoch
import me.masi.enums.EFilteringParameter
import me.masi.enums.ESortAttribute
import me.masi.enums.ESortDirection
import me.masi.services.inputreaders.api.InputReader
import me.masi.services.parsers.RankedLandsParser
import me.masi.services.parsers.api.EpochsParser

/**
 * Returns filtered stats.
 *
 * Features:
 *  - specify filtering parameter (player, alliance, state system, land number)
 *  - specify filtering query (string based on filtering parameter)
 *  - specify sort attribute (prestige or area)
 *  - specify sort direction
 *  - specify returned rows
 *  - specify epoch start
 *  - specify epoch end
 *  - specify rank start
 *  - specify rank end
 */
class FilterRankedLandsProcessor(
    inputReader: InputReader,
    private val parser: EpochsParser<RankedLandsEpoch> = RankedLandsParser(),
) : AbstractRankedLandProcessor(inputReader) {

    override fun process() {
        val filteringParameter = inputReader.selectFilteringParameterFromInput()
        val filteringQuery = when (filteringParameter) {
            EFilteringParameter.PLAYER -> inputReader.selectFilterPlayerQueryFromInput()
            EFilteringParameter.ALLIANCE -> inputReader.selectFilterAllianceQueryFromInput()
            EFilteringParameter.STATE_SYSTEM -> inputReader.selectFilterStateSystemQueryFromInput()
            EFilteringParameter.LAND_NUMBER -> inputReader.selectFilterLandNumberQueryFromInput()
        }
        val sortAttribute = inputReader.selectSortAttributeFromInput()
        val SortDirection = inputReader.selectSortDirectionFromInput()
        val landsCount = inputReader.selectReturnCountFromInput()
        val epochStart = inputReader.selectStartEpochFromInput()
        val epochEnd = inputReader.selectEndEpochFromInput()
        val rankStart = inputReader.selectStartRankFromInput()
        val rankEnd = inputReader.selectEndRankFromInput()

        val epochs = parser.parse()
        val filteredEpochs = filterEpochs(epochs, epochStart, epochEnd)
        val filteredRanks = filterRanks(filteredEpochs, rankStart, rankEnd)

        val rankedLands = filteredRanks.flatMap { it.rankedLands }
            .let {
                when (filteringParameter) {
                    EFilteringParameter.PLAYER -> it.filter { it.playerName.lowercase() == filteringQuery!!.lowercase() }
                    EFilteringParameter.ALLIANCE -> it.filter { it.alliance?.lowercase() == filteringQuery?.ifBlank { null }?.lowercase() }
                    EFilteringParameter.STATE_SYSTEM -> it.filter { it.stateSystem.lowercase() == filteringQuery!!.lowercase() }
                    EFilteringParameter.LAND_NUMBER -> it.filter { it.landNumber == (filteringQuery!!.toIntOrNull() ?: 0) }
                }
            }
            .let {
                when (SortDirection) {
                    ESortDirection.ASCENDING -> {
                        when (sortAttribute) {
                            ESortAttribute.PRESTIGE -> it.sortedBy { it.prestige }
                            ESortAttribute.AREA -> it.sortedBy { it.area }
                        }
                    }
                    ESortDirection.DESCENDING -> {
                        when (sortAttribute) {
                            ESortAttribute.PRESTIGE -> it.sortedByDescending { it.prestige }
                            ESortAttribute.AREA -> it.sortedByDescending { it.area }
                        }
                    }
                }
            }
            .let {
                if (landsCount != 0) {
                    it.take(landsCount)
                } else {
                    it.toList()
                }
            }

        processOutput(rankedLands, filteringParameter, filteringQuery)
    }

    private fun processOutput(rankedLands: List<RankedLand>, filteringParameter: EFilteringParameter, filteringQuery: String?) {
        println("${filteringParameter.value}: $filteringQuery")
        if (rankedLands.isEmpty()) {
            println("Žádné výsledky.")
            return
        }

        when (filteringParameter) {
            EFilteringParameter.PLAYER -> processPlayerOutput(rankedLands)
            EFilteringParameter.ALLIANCE -> processAllianceOutput(rankedLands)
            EFilteringParameter.STATE_SYSTEM -> processStateSystemOutput(rankedLands)
            EFilteringParameter.LAND_NUMBER -> processLandNumberOutput(rankedLands)
        }
    }

    private fun processPlayerOutput(rankedLands: List<RankedLand>) {
        println("#\tPrestiž\tRozloha\tVláda\tAliance\tVěk\tUmístění")
        var i = 1
        rankedLands.forEach { rankedLand ->
            println(
                "${i++}.\t${rankedLand.prestige}\t${rankedLand.area}km2\t${rankedLand.stateSystem}\t${rankedLand.alliance}\t${rankedLand.epochNumber}\t" +
                        "${rankedLand.rank}."
            )
        }
    }

    private fun processAllianceOutput(rankedLands: List<RankedLand>) {
        println("#\tHráč\tPrestiž\tRozloha\tVláda\tVěk\tUmístění")
        var i = 1
        rankedLands.forEach { rankedLand ->
            println(
                "${i++}.\t${rankedLand.playerName}\t${rankedLand.prestige}\t${rankedLand.area}km2\t${rankedLand.stateSystem}\t${rankedLand.epochNumber}\t" +
                        "${rankedLand.rank}."
            )
        }
    }

    private fun processStateSystemOutput(rankedLands: List<RankedLand>) {
        println("#\tHráč\tPrestiž\tRozloha\tAliance\tVěk\tUmístění")
        var i = 1
        rankedLands.forEach { rankedLand ->
            println(
                "${i++}.\t${rankedLand.playerName}\t${rankedLand.prestige}\t${rankedLand.area}km2\t${rankedLand.alliance}\t${rankedLand.epochNumber}\t" +
                        "${rankedLand.rank}."
            )
        }
    }

    private fun processLandNumberOutput(rankedLands: List<RankedLand>) {
        println("#\tHráč\tPrestiž\tRozloha\tVláda\tAliance\tVěk\tUmístění")
        var i = 1
        rankedLands.forEach { rankedLand ->
            println(
                "${i++}.\t${rankedLand.playerName}\t${rankedLand.prestige}\t${rankedLand.area}km2\t${rankedLand.stateSystem}\t${rankedLand.alliance}\t" +
                        "${rankedLand.epochNumber}\t${rankedLand.rank}."
            )
        }
    }
}
