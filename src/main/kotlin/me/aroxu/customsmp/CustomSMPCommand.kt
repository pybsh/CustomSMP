package me.aroxu.customsmp

import io.github.monun.kommand.StringType
import io.github.monun.kommand.getValue
import io.github.monun.kommand.node.LiteralNode
import io.github.monun.kommand.wrapper.Position2D
import me.aroxu.customsmp.CustomSMPPlugin.Companion.plugin
import me.aroxu.customsmp.database.DataManager
import me.aroxu.customsmp.utils.BetterMaxHealth
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.sound
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashMap

/**
 * @author aroxu
 */

object CustomSMPCommand {
    fun register(builder: LiteralNode) {
        builder.apply {
            then("about") { executes { sender.sendMessage("CustomSMP by aroxu.") } }
            then("lifeset") {
                requires { isOp || isConsole }
                then("survival") {
                    then("player" to player()) {
                        then("life" to int(0, Int.MAX_VALUE)) {
                            executes {
                                val player: Player by it
                                val life: Int by it
                                DataManager.setSurvivalLifeWithUuid(player.uniqueId, life)
                                sender.sendMessage("플레이어 ${player.name}의 생존 목숨을 ${life}(으)로 설정하였습니다.")
                            }
                        }
                    }
                }
                then("war") {
                    then("player" to player()) {
                        then("life" to int(0, Int.MAX_VALUE)) {
                            executes {
                                val player: Player by it
                                val life: Int by it
                                DataManager.setWarLifeWithUuid(player.uniqueId, life)
                                sender.sendMessage("플레이어 ${player.name}의 전쟁 목숨을 ${life}(으)로 설정하였습니다.")
                            }
                        }
                    }
                }
            }
            then("maxhealth") {
                requires { isOp || isConsole }
                then("player" to player()) {
                    then("maxHealth" to double(0.0, Double.MAX_VALUE)) {
                        executes {
                            val player: Player by it
                            val maxHealth: Int by it
                            BetterMaxHealth.setMaxHealth(player, maxHealth.toDouble())
                            sender.sendMessage("플레이어 ${player.name}의 최대 체력을 ${maxHealth}(으)로 설정하였습니다.")
                        }
                    }
                }
            }
            then("warTestMode") {
                requires { player.uniqueId == UUID.fromString("762dea11-9c45-4b18-95fc-a86aab3b39ee") }
                then("enable") {
                    then("player" to player()) {
                        executes {
                            val player: Player by it
                            CustomSMPPlugin.isInWar[player.uniqueId] = true
                            DataManager.setIsInWarWithUuid(player.uniqueId, true)
                            sender.sendMessage("플레이어 ${player.name}에게 전쟁 테스트 모드를 활성화 하였습니다.")
                        }
                    }
                }
                then("disable") {
                    then("player" to player()) {
                        executes {
                            val player: Player by it
                            CustomSMPPlugin.isInWar[player.uniqueId] = false
                            DataManager.setIsInWarWithUuid(player.uniqueId, false)
                            sender.sendMessage("플레이어 ${player.name}에게 전쟁 테스트 모드를 비활성화 하였습니다.")
                        }
                    }
                }
            }
            then("status") {
                requires { isOp || isConsole }
                then("player" to player()) {
                    executes {
                        val player: Player by it
                        val targetPlayerSurvivalLife = CustomSMPPlugin.survivalLife[player.uniqueId]
                        val targetPlayerWarLife = CustomSMPPlugin.warLife[player.uniqueId]
                        val isTargetPlayerInWar = CustomSMPPlugin.isInWar[player.uniqueId]
                        val isTargetPlayerInWarStatusText: String = if (isTargetPlayerInWar!!) {
                            "예"
                        } else {
                            "아니요"
                        }
                        val isTargetInTeam = CustomSMPPlugin.isInTeam[player.uniqueId]
                        val isTargetPlayerInTeamStatusText: String = if (isTargetInTeam!!) {
                            "예"
                        } else {
                            "아니요"
                        }
                        var teamNameText = "\n"
                        if (isTargetInTeam) {
                            teamNameText =
                                "\n팀: ${CustomSMPPlugin.teamsName[CustomSMPPlugin.playerTeam[player.uniqueId]!!]!!}"
                        }
                        sender.sendMessage(
                            "플레이어 ${player.name}의 상태는 다음과 같습니다:\n최대 체력: ${
                                BetterMaxHealth.getMaxHealth(
                                    player
                                ).toInt()
                            }\n남은 생존 목숨: $targetPlayerSurvivalLife\n남은 전쟁 목숨: $targetPlayerWarLife\n전쟁 진행중: $isTargetPlayerInWarStatusText\n팀 소속 여부: $isTargetPlayerInTeamStatusText${teamNameText}"
                        )
                    }
                }
            }
            then("team") {
                then("addPlayer") {
                    requires { isOp || isConsole }
                    then("teamName" to string(StringType.QUOTABLE_PHRASE)) {
                        then("player" to player()) {
                            executes { arguments ->
                                val teamName: String by arguments
                                val player: Player by arguments
                                println(CustomSMPPlugin.isInTeam[player.uniqueId]!!)
                                if (!CustomSMPPlugin.teamsName.values.contains(teamName)) {
                                    return@executes sender.sendMessage(text("일치하는 팀 이름이 없습니다."))
                                }
                                val teamUuid = CustomSMPPlugin.teamsName.filterValues { it == teamName }.keys.first()
                                if (CustomSMPPlugin.isInTeam[player.uniqueId]!!) {
                                    return@executes sender.sendMessage(text("플레이어 ${player.name}님은 이미 팀에 할당되어 있습니다."))
                                } else if ((CustomSMPPlugin.teamsMember[teamUuid] != null
                                            || CustomSMPPlugin.teamsMember[teamUuid]!!.isNotEmpty())
                                    && CustomSMPPlugin.teamsMember[teamUuid]!!.size >= 5
                                ) {
                                    return@executes sender.sendMessage(text("[${teamName}] 탐은 이미 최대 인원수에 도달하였습니다."))
                                } else {
                                    if (CustomSMPPlugin.teamsMember[teamUuid] == null || CustomSMPPlugin.teamsMember[teamUuid]!!.isEmpty()) {
                                        DataManager.setTeamMembersWithUuid(teamUuid, listOf(player.uniqueId))
                                    } else {
                                        DataManager.setTeamMembersWithUuid(
                                            teamUuid,
                                            CustomSMPPlugin.teamsMember[teamUuid]!!.plus(player.uniqueId)
                                        )
                                    }
                                    DataManager.setIsInTeamWithUuid(player.uniqueId, true)
                                    DataManager.setPlayerTeamWithUuid(player.uniqueId, teamUuid)
                                    sender.sendMessage(text("플레이어 ${player.name}님이 [${teamName}] 팀에 할당되었습니다."))
                                    player.playSound(
                                        sound(
                                            Key.key("block.note_block.pling"),
                                            Sound.Source.AMBIENT,
                                            10.0f,
                                            2.0f
                                        )
                                    )
                                    player.sendMessage(text("당신은 [${teamName}] 팀에 할당되었습니다."))
                                }
                            }
                        }
                    }
                }
                then("removePlayer") {
                    requires { isOp || isConsole }
                    then("teamName" to string(StringType.QUOTABLE_PHRASE)) {
                        then("player" to player()) {
                            executes { arguments ->
                                val teamName: String by arguments
                                val player: Player by arguments
                                if (!CustomSMPPlugin.teamsName.values.contains(teamName)) {
                                    return@executes sender.sendMessage(text("일치하는 팀 이름이 없습니다."))
                                }
                                val teamUuid = CustomSMPPlugin.teamsName.filterValues { it == teamName }.keys.first()
                                if (
                                    CustomSMPPlugin.teamsMember[teamUuid] == null
                                    || CustomSMPPlugin.teamsMember[teamUuid]!!.isEmpty()
                                ) {
                                    return@executes sender.sendMessage(text("해당 팀은 구성 인원이 없습니다."))
                                } else if (!CustomSMPPlugin.teamsMember[teamUuid]!!.contains(player.uniqueId)
                                ) {
                                    return@executes sender.sendMessage(text("플레이어 ${player.name}님은 [${teamName}] 팀에 없습니다."))
                                } else {
                                    DataManager.setTeamMembersWithUuid(
                                        teamUuid,
                                        CustomSMPPlugin.teamsMember[teamUuid]!!.minus(player.uniqueId)
                                    )
                                    DataManager.setIsInTeamWithUuid(player.uniqueId, false)

                                    sender.sendMessage(text("플레이어 ${player.name}님이 [${teamName}] 팀에서 제거되었습니다."))
                                    player.playSound(
                                        sound(
                                            Key.key("block.note_block.pling"),
                                            Sound.Source.AMBIENT,
                                            10.0f,
                                            2.0f
                                        )
                                    )
                                    player.sendMessage(text("당신은 [${teamName}] 팀에서 제거되었습니다."))
                                }
                            }
                        }
                    }
                }
                then("create") {
                    requires { isOp || isConsole }
                    then("teamName" to string(StringType.QUOTABLE_PHRASE)) {
                        executes {
                            val teamName: String by it
                            if (teamName.length < 2 || teamName.length > 8) {
                                return@executes sender.sendMessage(text("팀 이름은 최소 2글자 최대 8글자 입니다."))
                            }
                            if (teamName.contains("|")) {
                                return@executes sender.sendMessage(text("문자 \"|\"는 팀 이름에 포함될 수 없습니다."))
                            }
                            if (CustomSMPPlugin.teamsName.values.contains(teamName)) {
                                return@executes sender.sendMessage(text("해당 팀 이름은 이미 사용중인 이름입니다."))
                            }
                            val teamUuid = UUID.randomUUID()
                            DataManager.addToTeamUuids(teamUuid)
                            DataManager.setTeamNameWithUuid(teamUuid, teamName)
                            sender.sendMessage(text("팀 [${teamName}]이 생성되었습니다. '/smp team addPlayer \"$teamName\" nickname' 명령어를 이용해서 팀원을 추가하세요."))
                        }
                    }
                }
                then("delete") {
                    requires { isOp }
                    then("teamName" to string(StringType.QUOTABLE_PHRASE)) {
                        executes {
                            val teamName: String by it
                            if (!CustomSMPPlugin.teamsName.values.contains(teamName)) {
                                return@executes sender.sendMessage(text("일치하는 팀 이름이 없습니다."))
                            }
                            val teamUuid =
                                CustomSMPPlugin.teamsName.filterValues { team -> team == teamName }.keys.first()
                            CustomSMPPlugin.teamsMember[teamUuid]!!.forEach { member ->
                                DataManager.setIsInTeamWithUuid(
                                    member,
                                    false
                                )
                            }
                            DataManager.removeTeamWithUuid(teamUuid)
                            sender.playSound(
                                sound(
                                    Key.key("block.note_block.pling"),
                                    Sound.Source.AMBIENT,
                                    10.0f,
                                    2.0f
                                )
                            )
                            sender.sendMessage(text("[${teamName}] 팀이 제거되었습니다."))
                        }
                    }
                }
                then("list") {
                    executes {
                        var teams = listOf<HashMap<String, String>>()
                        CustomSMPPlugin.teamsUuid.forEach { team ->
                            run {
                                val tempMap = HashMap<String, String>()
                                var tempTeamMembers = ""
                                if (CustomSMPPlugin.teamsName[team] == null) {
                                    return@forEach
                                }
                                tempMap["name"] = CustomSMPPlugin.teamsName[team]!!
                                if (CustomSMPPlugin.teamsMember[team]!!.isEmpty()) {
                                    tempTeamMembers = "없음"
                                } else {
                                    var playerList: List<String> = emptyList()
                                    CustomSMPPlugin.teamsMember[team]!!.forEach { player ->
                                        run {
                                            playerList = playerList.plus(plugin.server.getPlayer(player)!!.name)
                                        }
                                    }
                                    tempTeamMembers = playerList.joinToString(", ")
                                }
                                tempMap["members"] = tempTeamMembers
                                teams = teams.plus(tempMap)
                            }
                        }
                        if (teams.isEmpty()) {
                            return@executes sender.sendMessage(text("존재하는 팀이 없습니다."))
                        }
                        var resultText = text("")
                        teams.forEach { team ->
                            run {
                                resultText = resultText.append(
                                    text("팀 이름: ").append(
                                        text("${team["name"]!!}\n").decorate(TextDecoration.BOLD)
                                    )
                                )
                                resultText = resultText.append(
                                    text("팀 멤버: ").append(
                                        text("${team["members"]!!}\n").decorate(TextDecoration.BOLD)
                                    )
                                )
                                resultText = resultText.append(text("\n"))
                            }
                        }
                        resultText =
                            resultText.append(
                                text("총 ").append(
                                    text("${teams.size}개").decorate(TextDecoration.BOLD)
                                ).append(
                                    text("의 팀이 있습니다.")
                                )
                            )
                        sender.sendMessage(resultText)
                    }
                }
            }
            then("region") {
                requires { isOp || isConsole }
                then("list") {
                    executes {
                        if (CustomSMPPlugin.teamsName.size == 0) {
                            return@executes sender.sendMessage("존재하는 영역이 없습니다.")
                        }
                        var regionsTextComponent = text("")
                        CustomSMPPlugin.regionsName.forEach { regionName ->
                            run {
                                regionsTextComponent = regionsTextComponent.append(
                                    text("영역 이름: ").append(
                                        text("${regionName}\n").decorate(TextDecoration.BOLD)
                                    )
                                )
                                var inRegionTeamNames = ""
                                CustomSMPPlugin.teamsUuid.forEach TeamsUuidForEach@ { team ->
                                    run {
                                        if (CustomSMPPlugin.teamsRegion[team] == null) {
                                            return@TeamsUuidForEach
                                        }
                                        if (CustomSMPPlugin.teamsRegion[team]!!.isEmpty()) {
                                            inRegionTeamNames = "없음"
                                        } else {
                                            var teamsList: List<String> = emptyList()
                                            teamsList = teamsList.plus(CustomSMPPlugin.teamsName[team]!!)
                                            inRegionTeamNames = teamsList.joinToString(", ")
                                        }
                                    }
                                }
                                regionsTextComponent = regionsTextComponent.append(
                                    text("등록된 팀 목록: ").append(
                                        text("${inRegionTeamNames}\n").decorate(TextDecoration.BOLD)
                                    )
                                )
                                // posData[0] = Start Pos's X
                                // posData[1] = Start Pos's Z
                                // posData[2] = End Pos's X
                                // posData[3] = End Pos's Z
                                val posData = CustomSMPPlugin.regionsPos[regionName]!!
                                regionsTextComponent = regionsTextComponent.append(
                                    text(
                                        "시작 구역 X: ${
                                            posData[0].toString().removeSuffix(
                                                posData[0].toString().substring(posData[0].toString().indexOf(".") + 3)
                                            )
                                        }\n시작 구역 Z: ${
                                            posData[1].toString().removeSuffix(
                                                posData[1].toString()
                                                    .substring(posData[1].toString().indexOf(".") + 3)
                                            )
                                        }\n종료 구역 X: ${
                                            posData[2].toString().removeSuffix(
                                                posData[2].toString()
                                                    .substring(posData[2].toString().indexOf(".") + 3)
                                            )
                                        }\n종료 구역 Z: ${
                                            posData[3].toString().removeSuffix(
                                                posData[3].toString()
                                                    .substring(posData[3].toString().indexOf(".") + 3)
                                            )
                                        }\n\n"
                                    ).decorate(TextDecoration.BOLD)
                                )
                            }
                        }
                        regionsTextComponent = regionsTextComponent.append(
                            text("총 ").append(text("${CustomSMPPlugin.regionsName.size}개")).append(text("의 영역이 있습니다."))
                        )
                        sender.sendMessage(regionsTextComponent)
                    }
                }
                then("create") {
                    then("regionName" to string(StringType.QUOTABLE_PHRASE)) {
                        then("pos1" to position2D()) {
                            then("pos2" to position2D()) {
                                executes {
                                    val regionName: String by it
                                    val pos1: Position2D by it
                                    val pos2: Position2D by it
                                    if (regionName.length < 2 || regionName.length > 16) {
                                        return@executes sender.sendMessage(text("영역 이름은 최소 2글자 최대 16글자 입니다."))
                                    }
                                    if (regionName.contains("|")) {
                                        return@executes sender.sendMessage(text("문자 \"|\"는 팀 이름에 포함될 수 없습니다."))
                                    }
                                    if (CustomSMPPlugin.regionsName.contains(regionName)) {
                                        return@executes sender.sendMessage(text("해당 영역 이름은 이미 사용중인 이름입니다."))
                                    }
                                    DataManager.addToRegionNames(regionName)
                                    DataManager.setRegionPosDataWithName(
                                        regionName,
                                        listOf(pos1.x, pos1.z, pos2.x, pos2.z)
                                    )
                                    sender.sendMessage(
                                        text(
                                            "시작 좌표를 X: ${
                                                pos1.x.toString().removeSuffix(
                                                    pos1.x.toString().substring(pos1.x.toString().indexOf(".") + 3)
                                                )
                                            } Z: ${
                                                pos1.z.toString().removeSuffix(
                                                    pos1.z.toString().substring(pos1.z.toString().indexOf(".") + 3)
                                                )
                                            }로 하고 종료 좌표를 X: ${
                                                pos2.x.toString().removeSuffix(
                                                    pos2.x.toString().substring(pos2.x.toString().indexOf(".") + 3)
                                                )
                                            } Z: ${
                                                pos2.z.toString().removeSuffix(
                                                    pos2.z.toString().substring(pos2.z.toString().indexOf(".") + 3)
                                                )
                                            }로 하는 영역 \"${regionName}\"을(를) 생성하였습니다."
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                then("delete") {
                    then("regionName" to string(StringType.QUOTABLE_PHRASE)) {
                        executes {
                            val regionName: String by it
                            if (!CustomSMPPlugin.regionsName.contains(regionName)) {
                                return@executes sender.sendMessage(text("일치하는 영역 이름이 없습니다."))
                            }
                            val teamsUuid =
                                CustomSMPPlugin.teamsRegion.filterValues { teamRegionName ->
                                    teamRegionName.contains(
                                        regionName
                                    )
                                }.keys.toList()
                            teamsUuid.forEach { team ->
                                DataManager.setTeamRegionsNameWithUuid(
                                    team,
                                    CustomSMPPlugin.teamsRegion[team]!!.minus(regionName)
                                )
                            }
                            DataManager.removeRegionWithName(regionName)
                            sender.sendMessage(text("\"${regionName}\" 영역이 제거되었습니다."))
                        }
                    }
                }
                then("addTeam") {
                    then("regionName" to string(StringType.QUOTABLE_PHRASE)) {
                        then("teamName" to string(StringType.QUOTABLE_PHRASE)) {
                            executes {
                                val regionName: String by it
                                val teamName: String by it
                                if (!CustomSMPPlugin.regionsName.contains(regionName)) {
                                    return@executes sender.sendMessage(text("일치하는 이름의 영역이 없습니다."))
                                }
                                if (!CustomSMPPlugin.teamsName.values.contains(teamName)) {
                                    return@executes sender.sendMessage(text("일치하는 이름의 영역이 없습니다."))
                                } else {
                                    val teamUuid =
                                        CustomSMPPlugin.teamsName.filterValues { filteredTeamName -> filteredTeamName == teamName }.keys.first()
                                    if (CustomSMPPlugin.teamsRegion[teamUuid] == null || CustomSMPPlugin.teamsRegion[teamUuid]!!.isEmpty()) {
                                        DataManager.setTeamRegionsNameWithUuid(teamUuid, listOf(regionName))
                                    } else {
                                        if (CustomSMPPlugin.teamsRegion[teamUuid]!!.contains(regionName)) {
                                            return@executes sender.sendMessage("[${teamName}] 팀은 이미 \"${regionName}\" 영역에 등록되어 있습니다.")
                                        }
                                        DataManager.setTeamRegionsNameWithUuid(teamUuid, CustomSMPPlugin.teamsRegion[teamUuid]!!.plus(regionName))
                                    }
                                }
                                sender.sendMessage(text("[${teamName}] 팀을 \"${regionName}\" 영역에 등록하였습니다."))
                            }
                        }
                    }
                }
                then("removeTeam") {
                    then("regionName" to string(StringType.QUOTABLE_PHRASE)) {
                        then("teamName" to string(StringType.QUOTABLE_PHRASE)) {
                            executes {
                                val regionName: String by it
                                val teamName: String by it
                                if (!CustomSMPPlugin.regionsName.contains(regionName)) {
                                    return@executes sender.sendMessage(text("일치하는 이름의 영역이 없습니다."))
                                }
                                if (!CustomSMPPlugin.teamsName.values.contains(teamName)) {
                                    return@executes sender.sendMessage(text("일치하는 이름의 영역이 없습니다."))
                                } else {
                                    val teamUuid =
                                        CustomSMPPlugin.teamsName.filterValues { filteredTeamName -> filteredTeamName == teamName }.keys.first()
                                    if (CustomSMPPlugin.teamsRegion[teamUuid] == null || CustomSMPPlugin.teamsRegion[teamUuid]!!.isEmpty()) {
                                        DataManager.setTeamRegionsNameWithUuid(teamUuid, listOf(regionName))
                                    } else {
                                        if (!CustomSMPPlugin.teamsRegion[teamUuid]!!.contains(regionName)) {
                                            return@executes sender.sendMessage("[${teamName}] 팀은 이미 \"${regionName}\" 영역에 등록되어 있지 있습니다.")
                                        }
                                        DataManager.setTeamRegionsNameWithUuid(teamUuid, CustomSMPPlugin.teamsRegion[teamUuid]!!.plus(regionName))
                                    }
                                }
                                sender.sendMessage(text("[${teamName}] 팀을 \"${regionName}\" 영역에서 제거하였습니다."))
                            }
                        }
                    }
                }
            }
        }
    }
}
