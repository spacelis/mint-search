package uk.ac.cdrc.mintsearch.search

import uk.ac.cdrc.mintsearch.graph.{ExponentialPropagation, NeighbourAwareContext, NeighbourhoodByRadius}
import uk.ac.cdrc.mintsearch.index.{BaseIndexManager, KeyValueLabelType, PropertyLabelMaker}

/**
  * These are just sample configurations and can be ignored when
  * building a Searcher.
  */

/**
  * A configuration trait defines part of the indexing/search parameters
  * It reads Radius 2 EXPonential propagation PROPerty Label maker with InDeX base
  */
trait ConfR2expPropLIdx extends PropertyLabelMaker with KeyValueLabelType with NeighbourhoodByRadius with ExponentialPropagation with BaseIndexManager with NeighbourAwareContext {
  override val radius = 2
  override val propagationFactor: Double = 0.5
  override val indexName: String = s"index-nagg-r$radius-p$propagationFactor"
  override val labelStorePropKey: String = s"__nagg_$radius"
}
