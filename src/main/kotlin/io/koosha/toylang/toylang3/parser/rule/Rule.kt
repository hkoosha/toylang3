package io.koosha.toylang.toylang3.parser.rule

import io.koosha.toylang.toylang3.lexer.TokenKind
import io.koosha.toylang.toylang3.util.isInt
import io.koosha.toylang.toylang3.util.removeFirstByOrNull
import java.util.regex.Pattern

class Rule(
    val name: String,
    val num: Int,
    private val alternatives: MutableList<List<RulePart>>,
    val first: Set<RulePart>,
    val follow: Set<RulePart>,
) : Comparable<Rule> {

    private fun validate(): Rule {

        val alts: Set<List<String>> =
            this.alternatives
                .asSequence()
                .map {
                    it
                        .asSequence()
                        .map(RulePart::name)
                        .toList()
                }
                .toSet()

        if (alts.size != this.alternatives.size)
            error("duplicate alternatives in rule: $this")

        if (!this.alternatives.any { it.isEmpty() || it[0].isToken() || it[0].name() != this.name })
            error("infinitely recursive rule: $this")

        if (this.alternatives.any { it.size > 1 && it.contains(RulePart.of(TokenKind.Epsilon)) })
            error("alternative with more than 1 element contains epsilon: $this")

        if (this.alternatives.isEmpty())
            error("empty rule: $this")

        if (this.first.isEmpty())
            error("empty first set: $this")

        // if (!this.isToken() && this.follow.isEmpty())
        //     error("empty follow set: $this")

        return this
    }

    @Suppress("unused")
    fun alternatives(): List<List<RulePart>> =
        this.alternatives

    @Suppress("unused")
    fun isToken(): Boolean =
        this.alternatives.size == 1 && this.alternatives[0].size == 1 && this.alternatives[0][0].isToken()


    override fun compareTo(other: Rule): Int =
        this.num.compareTo(other.num)


    override fun equals(other: Any?): Boolean {

        if (this === other)
            return true
        if (javaClass != other?.javaClass)
            return false

        other as Rule

        return this.name == other.name &&
                this.num == other.num
    }

    override fun hashCode(): Int {

        var result = this.name.hashCode()
        result = 31 * result + this.num
        return result
    }

    override fun toString(): String {

        val alts = this.altsToString()

        val first: String =
            this.first
                .asSequence()
                .map(RulePart::repr)
                .joinToString(", ")
                .trim()

        val follow: String =
            this.follow
                .asSequence()
                .map(RulePart::repr)
                .joinToString(", ")
                .trim()

        return "Rule(name='${this.name}', num=${this.num}, alts=[$alts], first=[$first], follow=[$follow])"
    }

    fun altsToString(): String {

        val alts: String =
            this.alternatives
                .asSequence()
                .map { alt ->
                    alt
                        .asSequence()
                        .map { it.repr() }
                        .joinToString(" ")
                }
                .joinToString(" | ")
                .trim()

        return alts
    }


    private class RuleIntermediate(
        val name: String,
        val num: Int,
        val alternatives: MutableList<MutableList<RulePartIntermediate>> = mutableListOf(),
        val first: MutableSet<RulePartIntermediate> = mutableSetOf(),
        val follow: MutableSet<RulePartIntermediate> = mutableSetOf(),
    ) : Comparable<RuleIntermediate> {

        fun isToken(): Boolean =
            this.alternatives.size == 1 && this.alternatives[0].size == 1 && this.alternatives[0][0].isToken()

        override fun compareTo(other: RuleIntermediate): Int =
            this.num.compareTo(other.num)


        override fun equals(other: Any?): Boolean {

            if (this === other)
                return true
            if (javaClass != other?.javaClass)
                return false

            other as RuleIntermediate

            return this.name == other.name &&
                    this.num == other.num
        }

        override fun hashCode(): Int {

            var result = this.name.hashCode()
            result = 31 * result + this.num
            return result
        }

        override fun toString(): String {

            val alts: String =
                this.alternatives
                    .asSequence()
                    .map { alt ->
                        alt
                            .asSequence()
                            .map { it.repr() }
                            .joinToString(" ")
                    }
                    .joinToString(" | ")
                    .trim()

            return "RuleIntermediate(name='${this.name}', num=${this.num}, alts=[$alts])"
        }

    }

    private class RulePartIntermediate(
        val rule: RuleIntermediate?,
        val tokenKind: TokenKind?,
    ) {

        companion object {

            val EPSILON = of(TokenKind.Epsilon)

            fun of(tokenKind: TokenKind): RulePartIntermediate =
                RulePartIntermediate(rule = null, tokenKind = tokenKind)

            fun of(rule: RuleIntermediate): RulePartIntermediate =
                RulePartIntermediate(rule = rule, tokenKind = null)

        }

        init {
            if (rule != null && tokenKind != null)
                error("can not set rule and token at the same time")

            if (rule == null && tokenKind == null)
                error("rule and token can not be null at the same time")
        }

        fun isToken(): Boolean =
            this.tokenKind != null

        fun repr(): String =
            this.rule?.name ?: this.tokenKind!!.repr ?: this.tokenKind!!.name

        fun first(): Set<RulePartIntermediate> =
            if (this.isToken())
                setOf(this)
            else
                this.rule!!.first

        override fun toString(): String {

            val type: String =
                if (isToken())
                    "token"
                else
                    "rule"

            return "RulePart(${type}=${this.repr()})"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other)
                return true
            if (javaClass != other?.javaClass)
                return false

            other as RulePartIntermediate

            return this.tokenKind == other.tokenKind &&
                    this.rule == other.rule
        }

        override fun hashCode(): Int =
            if (this.isToken())
                this.tokenKind!!.hashCode()
            else
                this.rule!!.hashCode()

    }


    /**
     * I know! you won't find the most efficient implementations or idioms here!
     */
    companion object {

        private val VALID_RULE_REGEX: Regex = Pattern.compile("^[a-zA-Z0-9_]+$").toRegex()

        fun isBacktrackFree(rules: List<Rule>): Boolean {

            rules.forEach { rule ->

                val startSets = mutableSetOf<Set<RulePart>>()

                for (alt: List<RulePart> in rule.alternatives) {
                    val start: Set<RulePart> =
                        if (alt[0].first().contains(RulePart.of(TokenKind.Epsilon)))
                            alt[0].first() - RulePart.of(TokenKind.Epsilon) + rule.follow
                        else
                            alt[0].first()

                    if (!startSets.add(start))
                        return false
                }
            }

            return true
        }

        fun parse(
            rulesDef: String,
            eliminateBacktracking: Boolean,
        ): List<Rule> {

            val rules = mutableListOf<RuleIntermediate>()

            var ruleNum = 0

            val ensureRule: (String) -> RuleIntermediate = { name: String ->

                if (!VALID_RULE_REGEX.matches(name))
                    error("invalid rule name: $name")

                if (name.endsWith("_p") || name.contains("_p") && name.split("_p").last().isInt())
                    error("numeric _p suffix is reserved and can not be part of the name: $name")

                var rule: RuleIntermediate? = rules.find { it.name == name }

                if (rule == null) {
                    rule = RuleIntermediate(name = name, num = ruleNum++)
                    rules.add(rule)
                }

                rule
            }

            for (tokenKind in TokenKind.values()) {
                if (tokenKind == TokenKind.Error || tokenKind == TokenKind.Eof || tokenKind == TokenKind.Epsilon)
                    continue

                val rule: RuleIntermediate = ensureRule(tokenKind.name.uppercase())
                rule.alternatives.add(mutableListOf())
                rule.alternatives.last().add(RulePartIntermediate.of(tokenKind))
            }

            rulesDef
                .lines()
                .asSequence()
                .map { it.substringBefore('#') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.split("->", limit = 2) }
                .map { it[0].trim() to it[1].trim() }
                .forEach { (ruleName: String, ruleDef: String) ->

                    val rule: RuleIntermediate = ensureRule(ruleName)

                    ruleDef.split('|').asSequence().map(String::trim).forEach { altParts: String ->

                        rule.alternatives.add(mutableListOf())

                        altParts.split(' ').asSequence().map(String::trim).forEach { altPart ->

                            val tokenPart: TokenKind? = TokenKind.fromReprOrEpsilonElseNull(altPart)
                            if (tokenPart != null)
                                rule.alternatives.last().add(RulePartIntermediate.of(tokenPart))
                            else
                                rule.alternatives.last().add(RulePartIntermediate.of(ensureRule(altPart)))
                        }
                    }
                }

            if (rules.count { it.name == "start" } == 0)
                error("no 'start' rule defined")

            this.eliminateEpsilon(rules)
            this.eliminateLeftRecursion(rules)

            if (eliminateBacktracking) {
                this.leftExpansion(rules)
                this.eliminateBacktracking(rules)
                this.eliminateDuplicates(rules)
            }

            this.setFirst(rules)
            this.setFollow(rules)

            val externalized: List<Rule> = this.toExternal(rules)
            externalized.forEach(Rule::validate)

            val seenName = mutableSetOf<String>()
            for (rule in externalized)
                if (!seenName.add(rule.name))
                    error("duplicate rule name: $rule")

            // val seenRule = mutableSetOf<Set<List<RulePart>>>()
            // for (rule in externalized)
            //     if (!seenRule.add(rule.alternatives.toSet()))
            //         error("duplicate rule: $rule")

            return externalized
        }

        private fun leftExpansion(rules: MutableList<RuleIntermediate>) {

            do {
                var anyChange = false

                rules.forEach { rule: RuleIntermediate ->

                    val expansionPoint: MutableList<RulePartIntermediate>? = rule.alternatives.removeFirstByOrNull {
                        !it[0].isToken()
                    }

                    if (expansionPoint != null) {

                        expansionPoint.removeFirst().rule!!.alternatives.forEach { alt: MutableList<RulePartIntermediate> ->
                            rule.alternatives.add(mutableListOf())
                            rule.alternatives.last().addAll(alt)
                            rule.alternatives.last().addAll(expansionPoint)
                        }

                        anyChange = true
                    }
                }

            } while (anyChange)
        }

        private fun getNewRule(
            rules: MutableList<RuleIntermediate>,
            name: String,
        ): RuleIntermediate {

            if (!VALID_RULE_REGEX.matches(name))
                error("invalid rule name: $name")

            val namePrefix: String
            val nameSuffix: Int
            if (name.contains("_p") && name.split("_p").last().isInt()) {
                val parts: List<String> = name.split("_p")
                namePrefix = parts.subList(0, parts.size - 1).joinToString("") + "_p"
                nameSuffix = parts.last().toInt() + 1
            }
            else {
                namePrefix = name + "_p"
                nameSuffix = 0
            }

            val ruleName: String = namePrefix + nameSuffix

            if (rules.findLast { it.name == ruleName } != null)
                return this.getNewRule(rules, ruleName)

            val rule = RuleIntermediate(name = ruleName, num = rules.maxBy { it.num }.num + 1)
            rules.add(rule)

            return rule
        }


        private fun eliminateDuplicates(rules: MutableList<RuleIntermediate>) {

            do {

                var foundI: RuleIntermediate? = null
                var foundJ: RuleIntermediate? = null

                outer@
                for (i in rules.size - 1 downTo 1)
                    for (j in i - 1 downTo 0) {
                        val ruleI = rules[i]
                        val ruleJ = rules[j]

                        if (ruleI.alternatives.toSet() == ruleJ.alternatives.toSet()) {
                            foundI = ruleI
                            foundJ = ruleJ
                            break@outer
                        }
                    }

                if (foundI != null) {
                    rules.remove(foundJ!!)
                    rules.forEach { rule: RuleIntermediate ->
                        rule.alternatives.forEach { alt: MutableList<RulePartIntermediate> ->
                            alt.replaceAll {
                                if (it.rule == foundJ)
                                    RulePartIntermediate.of(foundI)
                                else
                                    it
                            }
                        }
                    }
                }

            } while (foundI != null)

        }

        private fun eliminateBacktracking(rules: MutableList<RuleIntermediate>) {

            var loops = -1
            do {
                var newRule: RuleIntermediate? = null

                for (rule: RuleIntermediate in rules) {

                    var foundAlt: MutableList<RulePartIntermediate>? = null
                    var index = -1

                    foundAlt@
                    for (alt: MutableList<RulePartIntermediate> in rule.alternatives)
                        for (comparingAlt: MutableList<RulePartIntermediate> in rule.alternatives)
                            if (alt !== comparingAlt && alt.size < comparingAlt.size)
                                for (i in alt.size downTo 1)
                                    if (alt.subList(0, i) == comparingAlt.subList(0, i)) {
                                        foundAlt = alt
                                        index = i
                                        break@foundAlt
                                    }

                    if (foundAlt != null) {
                        val commonPrefix: List<RulePartIntermediate> = foundAlt.toList().subList(0, index)

                        newRule = this.getNewRule(rules, rule.name)

                        for (alt: MutableList<RulePartIntermediate> in rule.alternatives)
                            if (alt.size >= index && alt.subList(0, index) == commonPrefix) {
                                val suffix = alt.subList(index, alt.size)
                                newRule.alternatives.add(mutableListOf())
                                if (suffix.isEmpty())
                                    newRule.alternatives.last().add(RulePartIntermediate.EPSILON)
                                else
                                    newRule.alternatives.last().addAll(suffix)
                                alt.clear()
                            }

                        rule.alternatives.add(mutableListOf())
                        rule.alternatives.last().addAll(commonPrefix)
                        rule.alternatives.last().add(RulePartIntermediate.of(newRule))

                        rule.alternatives.retainAll { it.isNotEmpty() }

                        break
                    }
                }

                if (newRule != null && loops++ >= 1024)
                    error("could not eliminate backtracking: $rules")
            } while (newRule != null)
        }

        // Changes program semantics!! doesn't handle self-referencing rules with epsilons properly.
        // Just removes epsilon on them.
        private fun eliminateEpsilon(rules: MutableList<RuleIntermediate>) {

            do {
                var found: RuleIntermediate? = null

                for (rule: RuleIntermediate in rules)
                    if (rule.alternatives.removeAll { it.size == 1 && it[0] == RulePartIntermediate.EPSILON }) {
                        found = rule
                        break
                    }

                if (found != null)
                    for (rule: RuleIntermediate in rules) {
                        if (rule == found)
                            continue

                        val newRules = mutableListOf<MutableList<RulePartIntermediate>>()
                        val oldRules = mutableListOf<MutableList<RulePartIntermediate>>()

                        rule.alternatives.forEach { it: MutableList<RulePartIntermediate> ->

                            val indexes = mutableListOf<Int>()
                            for ((index: Int, used: RulePartIntermediate) in it.withIndex())
                                if (used == RulePartIntermediate.of(found))
                                    indexes.add(index)

                            if (indexes.size > 4)
                                error("can not handle more than 4 epsilon elimination rules: $rule")

                            if (indexes.isNotEmpty()) {

                                oldRules.add(it)

                                val max = 1L shl indexes.size
                                var removeMask = 0L

                                while (removeMask < max) {

                                    val newRule = mutableListOf<RulePartIntermediate>()

                                    var index = -1
                                    it.forEach { part: RulePartIntermediate ->
                                        if (part.rule == found)
                                            index++
                                        if (part.rule != found || (1L shl index).and(removeMask) == 0L)
                                            newRule.add(part)
                                    }

                                    newRules.add(newRule)
                                    removeMask++
                                }
                            }
                        }

                        rule.alternatives.removeAll(oldRules)
                        rule.alternatives.addAll(newRules)
                    }

            } while (found != null)
        }

        private fun eliminateLeftRecursion(rules: MutableList<RuleIntermediate>) {

            rules.sort()

            var round = 0
            val maxRounds = 1024

            do {
                var anyChange = false

                for (i in 1..rules.last().num) {
                    val inspecting: RuleIntermediate = rules[i]

                    for (s in 0 until i) {
                        val alt: MutableList<RulePartIntermediate> =
                            inspecting.alternatives.find { !it[0].isToken() && it[0].rule!!.num == s }
                                ?: continue

                        inspecting.alternatives.remove(alt)

                        val recursion: RulePartIntermediate = alt.removeFirst()
                        recursion.rule!!.alternatives.forEach { recAlt: MutableList<RulePartIntermediate>? ->
                            val newRule: List<RulePartIntermediate> = recAlt!! + alt
                            inspecting.alternatives.add(newRule.toMutableList())
                        }

                        anyChange = true
                    }

                    val (
                        recursive0: List<MutableList<RulePartIntermediate>?>,
                        nonRecursive: List<MutableList<RulePartIntermediate>?>,
                    ) = inspecting.alternatives.partition { it[0].rule == inspecting }
                    val recursive = recursive0.toMutableList()

                    if (recursive.isNotEmpty()) {

                        val prime: RuleIntermediate = this.getNewRule(rules, inspecting.name)

                        nonRecursive.forEach {
                            // if (it.size != 1 || !it[0].isEpsilon())
                            it.add(RulePartIntermediate.of(prime))
                        }
                        inspecting.alternatives.clear()
                        inspecting.alternatives.addAll(nonRecursive)

                        recursive.forEach {
                            it.removeFirst()
                            // if (it.isNotEmpty() && it[0].isEpsilon())
                            //     it.removeFirst()
                            it.add(RulePartIntermediate.of(prime))
                        }
                        recursive.add(mutableListOf(RulePartIntermediate.EPSILON))
                        prime.alternatives.addAll(recursive)

                        anyChange = true
                    }
                }

                round++
                if (round > maxRounds)
                    error("could not eliminate left recursion after $round loops: $rules")
            } while (anyChange)
        }

        private fun setFirst(rules: MutableList<RuleIntermediate>) {

            rules.sort()

            rules.forEach {
                if (it.isToken())
                    it.first.add(it.alternatives[0][0])
            }

            do {
                var anyChange = false

                rules.forEach { rule: RuleIntermediate? ->

                    if (!rule!!.isToken())
                        rule.alternatives.forEach { alt: MutableList<RulePartIntermediate>? ->

                            val rhs: MutableSet<RulePartIntermediate> =
                                (alt!![0].first() - RulePartIntermediate.EPSILON).toMutableSet()

                            var trailing = true

                            for (i in 1 until alt.size - 1)
                                if (alt[i].first().contains(RulePartIntermediate.EPSILON)) {
                                    rhs += alt[i + 1].first() - RulePartIntermediate.EPSILON
                                }
                                else {
                                    trailing = false
                                    break
                                }

                            if (trailing && alt.last().first().contains(RulePartIntermediate.EPSILON))
                                rhs.add(RulePartIntermediate.EPSILON)

                            anyChange = rule.first.addAll(rhs) || anyChange
                        }
                }

            } while (anyChange)
        }

        private fun setFollow(rules: MutableList<RuleIntermediate>) {

            rules.find { it.name == "start" }!!.follow.add(RulePartIntermediate.of(TokenKind.Eof))

            do {
                var anyChange = false

                rules.forEach { rule: RuleIntermediate ->

                    rule.alternatives.forEach { alt: MutableList<RulePartIntermediate> ->

                        var trailer: MutableSet<RulePartIntermediate> = rule.follow.toMutableSet()

                        for (i in alt.size - 1 downTo 0) {

                            if (!alt[i].isToken()) {
                                anyChange = alt[i].rule!!.follow.addAll(trailer) || anyChange
                                if (alt[i].first().contains(RulePartIntermediate.EPSILON))
                                    trailer.addAll(alt[i].first() - RulePartIntermediate.EPSILON)
                                else
                                    trailer = alt[i].first().toMutableSet()
                            }
                            else {
                                trailer = mutableSetOf(alt[i])
                            }
                        }
                    }
                }

            } while (anyChange)
        }

        private fun toExternal(rules: List<RuleIntermediate>): List<Rule> {

            val mapped: List<Rule> = rules.map {
                Rule(
                    name = it.name,
                    num = it.num,
                    alternatives = mutableListOf(),
                    first = setOf(),
                    follow = setOf(),
                )
            }

            val withSets = mapped.map {
                Rule(
                    name = it.name,
                    num = it.num,
                    alternatives = it.alternatives,
                    first = rules.find { r -> r.name == it.name }!!.first.map { part ->
                        if (part.isToken())
                            RulePart.of(part.tokenKind!!)
                        else
                            RulePart.of(mapped.find { r -> r.name == part.rule!!.name }!!)
                    }.toSet(),
                    follow = rules.find { r -> r.name == it.name }!!.follow.map { part ->
                        if (part.isToken())
                            RulePart.of(part.tokenKind!!)
                        else
                            RulePart.of(mapped.find { r -> r.name == part.rule!!.name }!!)
                    }.toSet(),
                )
            }

            withSets.forEach { r ->
                rules.find { it.name == r.name }!!.alternatives.forEach { alt ->
                    val list = mutableListOf<RulePart>()
                    alt.forEach { part: RulePartIntermediate ->
                        if (part.isToken())
                            list.add(RulePart.of(part.tokenKind!!))
                        else
                            list.add(RulePart.of(withSets.find { it.name == part.rule!!.name }!!))
                    }
                    r.alternatives.add(list.toList())
                }
            }

            return withSets
        }

    }

}
