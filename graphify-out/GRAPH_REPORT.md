# Graph Report - C:\Users\charl\Projetos\wex-transactions  (2026-05-23)

## Corpus Check
- 21 files · ~2,553 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 81 nodes · 60 edges · 21 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]

## God Nodes (most connected - your core abstractions)
1. `TargetCurrency` - 5 edges
2. `TransactionDate` - 5 edges
3. `ArchitectureScaffoldTest` - 5 edges
4. `ExchangeRate` - 4 edges
5. `DependencyDirectionTest` - 4 edges
6. `UseCaseOwnershipTest` - 4 edges
7. `Purchase` - 3 edges
8. `PurchaseRepositoryPort` - 3 edges
9. `ArchitectureTest` - 3 edges
10. `ControllerBoundaryTest` - 3 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.29
Nodes (1): TransactionDate

### Community 1 - "Community 1"
Cohesion: 0.33
Nodes (5): DomainException, InvalidCurrencyException, PurchaseNotFoundException, RateStaleException, RateUnavailableException

### Community 2 - "Community 2"
Cohesion: 0.33
Nodes (1): TargetCurrency

### Community 3 - "Community 3"
Cohesion: 0.33
Nodes (1): ArchitectureScaffoldTest

### Community 4 - "Community 4"
Cohesion: 0.4
Nodes (1): ExchangeRate

### Community 5 - "Community 5"
Cohesion: 0.4
Nodes (1): DependencyDirectionTest

### Community 6 - "Community 6"
Cohesion: 0.4
Nodes (1): UseCaseOwnershipTest

### Community 7 - "Community 7"
Cohesion: 0.5
Nodes (1): Purchase

### Community 8 - "Community 8"
Cohesion: 0.5
Nodes (1): PurchaseRepositoryPort

### Community 9 - "Community 9"
Cohesion: 0.5
Nodes (1): ArchitectureTest

### Community 10 - "Community 10"
Cohesion: 0.5
Nodes (1): ControllerBoundaryTest

### Community 11 - "Community 11"
Cohesion: 0.5
Nodes (1): VacuousGuardTest

### Community 12 - "Community 12"
Cohesion: 0.67
Nodes (1): Application

### Community 13 - "Community 13"
Cohesion: 0.67
Nodes (1): RetrieveConvertedQueryPort

### Community 14 - "Community 14"
Cohesion: 0.67
Nodes (1): StorePurchaseCommandPort

### Community 15 - "Community 15"
Cohesion: 0.67
Nodes (1): ExchangeRateClientPort

### Community 16 - "Community 16"
Cohesion: 0.67
Nodes (1): ApplicationTests

### Community 17 - "Community 17"
Cohesion: 1.0
Nodes (1): RetrieveConvertedQuery

### Community 18 - "Community 18"
Cohesion: 1.0
Nodes (1): StorePurchaseCommand

### Community 19 - "Community 19"
Cohesion: 1.0
Nodes (0): 

### Community 20 - "Community 20"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **8 isolated node(s):** `Application`, `DomainException`, `PurchaseNotFoundException`, `RateUnavailableException`, `RateStaleException` (+3 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 17`** (2 nodes): `RetrieveConvertedQuery.kt`, `RetrieveConvertedQuery`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 18`** (2 nodes): `StorePurchaseCommand.kt`, `StorePurchaseCommand`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 19`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (1 nodes): `settings.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What connects `Application`, `DomainException`, `PurchaseNotFoundException` to the rest of the system?**
  _8 weakly-connected nodes found - possible documentation gaps or missing edges._