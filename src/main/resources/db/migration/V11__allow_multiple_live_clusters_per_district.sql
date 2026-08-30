-- Spatially distinct hotspots in the same district are separate live clusters.
-- Matching is ST_DWithin (500 m) in application code, not "one cluster per district".
DROP INDEX IF EXISTS uq_report_cluster_one_live_per_district;
