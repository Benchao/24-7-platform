<?php
require 'inc.php';
define('lodThemes', LOD.'themes/');

$lodTopics = array(

  'geographic',
  'government',
  'media',
  'crossdomain',
  'lifesciences',
  'usergeneratedcontent',
  'ecommerce',
  'schemata',

);


$latcStore = new Store(LATC_STORE_URI, new Credentials('latc', LATC_STORE_PASSWORD));

$RequestFactory = new HttpRequestFactory();
$SparqlEndpoint = new SparqlService("http://semantic.ckan.net/sparql/");


$Prefixes = json_decode($RequestFactory->make('GET', 'http://prefix.cc/popular/all.file.json')->execute()->body, 1);
$Prefixes = array_filter($Prefixes, create_function('$a', 'return $a;'));


$pageSize = 500;
if(file_exists('last_imported_from_ckan')){
  $lastModTime = date('c', filemtime('last_imported_from_ckan'));
  $modifiedClause = 'OPTIONAL { ?s dc:modified ?mod . FILTER(?mod > "'.$lastModTime.'"^^xsd:dateTime ) . } OPTIONAL { ?s dc:created ?created . FILTER(?created > "'.$lastModTime.'"^^xsd:dateTime ) . } ';
} else {
  $modifiedClause = '';
}

$licenseIDs = array();

$continue = true;
$offset = 0;
while($continue){

$query = <<<_SPARQL_
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX dc: <http://purl.org/dc/terms/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

select distinct ?s
WHERE {
  ?s dc:isPartOf <http://ckan.net/group/lodcloud> .
   {$modifiedClause}
}
LIMIT {$pageSize} OFFSET {$offset}
_SPARQL_;
$requests = array();
$results = $SparqlEndpoint->select_to_array($query);
/*
$results[]= array('s' => array('value' => 'http://ckan.net/package/latc-linksets'));
$results[]= array('s' => array('value' => 'http://ckan.net/package/data-incubator-metoffice'));
$results[]= array('s' => array('value' => 'http://ckan.net/package/data-incubator-smcjournals'));

$results = array_reverse($results);
 */

foreach($results as $row){
  $uri = $row['s']['value'];
  $request = $RequestFactory->make("GET", $uri);
  $request->set_accept('application/rdf+xml');
  $response = $request->execute();
    $graph = new SimpleGraph();
    $graph->add_rdf($response->body);
    $graph->add_resource_triple($uri, RDF_TYPE, 'http://rdfs.org/ns/void#Dataset');
    $graph->add_literal_triple($uri, OPENVOCAB.'canonicalUri', $uri);
    $ckanJSON = $graph->get_first_literal($uri, 'http://semantic.ckan.net/schema#json');
    $graph->remove_literal_triple($uri,  'http://semantic.ckan.net/schema#json', $ckanJSON);
    $ckanArray = json_decode($ckanJSON, true);
   $graphURIs = array();
    foreach($graph->get_index() as $s => $ps){
      if(strpos($s, 'http://ckan.net/package/')===0 OR strpos($s, 'http://ckan.net/tag')===0) $graphURIs[]=$s;
      foreach($ps as $p => $os){
        foreach($os as $o){
          if($o['type']=='uri'){
            if(strpos($o['value'], 'http://ckan.net/package')===0 OR strpos($o['value'], 'http://ckan.net/tag')===0) $graphURIs[]=$o['value'];
          }
        }
      }
    }
  $graphURIs = array_unique($graphURIs);
    foreach($graphURIs as $oldUri){
      if(strpos($oldUri, 'http://ckan.net/package/')===0) $lodCloudUri = str_replace('http://ckan.net/package/', 'http://lod-cloud.net/', $oldUri);
      else if(strpos($oldUri, 'http://ckan.net/tag/')===0) $lodCloudUri = str_replace('http://ckan.net/tag/', 'http://lod-cloud.net/tag/', $oldUri);
      $graph->replace_resource($oldUri, $lodCloudUri);
      $graph->add_resource_triple($lodCloudUri, OWL_SAMEAS, $oldUri);
    }

    $packageName = str_replace('http://ckan.net/package/', '', $uri);

    $uri = LOD.$packageName;

    $graph->add_literal_triple($uri, OPENVOCAB.'shortName', $packageName);

       if(isset($ckanArray['extras'])){
      if(isset($ckanArray['extras']['uriSpace'])){
        $graph->add_literal_triple($uri, VOID.'uriSpace', $ckanArray['extras']['uriSpace']);
      }
      else if(isset($ckanArray['extras']['namespace'])){
        $graph->add_literal_triple($uri, VOID.'uriSpace', $ckanArray['extras']['namespace']);
      }

      foreach(array('classes', 'entities','triples', 'properties', 'distinctSubjects', 'distinctObjects', 'documents') as $statProp){
        if(isset($ckanArray['extras'][$statProp])){
          $graph->add_literal_triple($uri, VOID.$statProp, intval($ckanArray['extras'][$statProp]), '',xsd.'integer' );
        }
      }
 
    }
  if(isset($ckanArray['title'])){
        $graph->add_literal_triple($uri, RDFS_LABEL,  $ckanArray['title']);
    }

    if(isset($ckanArray['resources'])){
      foreach($ckanArray['resources'] as $resource){
        if($resource['format'] == 'meta/rdf-schema'){
          $graph->add_resource_triple($uri, VOID.'vocabulary', rtrim($resource['url'], '#'));
        }
        if($resource['format'] == 'meta/void'){
          $graph->add_resource_triple($uri, RDFS_SEEALSO, $resource['url']);
        }
      }
    }

    
    foreach($ckanArray['tags'] as $tag){
      if(strpos($tag, 'format-')===0){
        $prefix = str_replace('format-', '', $tag);
        if(isset($Prefixes[$prefix])){
          $vocabUri = rtrim($Prefixes[$prefix], '#');
          $graph->add_literal_triple($vocabUri, RDFS_LABEL, $prefix);
          $graph->add_literal_triple($vocabUri, VANN.'preferredNamespacePrefix', $prefix);
          $graph->add_literal_triple($vocabUri, VANN.'preferredNamespaceUri', $Prefixes[$prefix]);
          $graph->add_resource_triple($uri, VOID.'vocabulary', $vocabUri);
        }
      } else if(in_array($tag, $lodTopics)) {
          $graph->add_resource_triple($uri, DCT.'subject', lodThemes.$tag);
      }
      
    }

    foreach($graph->get_subjects_of_type(VOID.'Linkset') as $s){
      if(!$graph->get_first_literal($s, RDFS_LABEL)){
        $hostTargets = $graph->get_subjects_where_resource(VOID.'subset', $s);
        $host = $hostTargets[0];
        $targets = $graph->get_resource_triple_values($s, VOID.'target');
        while($target = array_pop($targets)){  if($host!=$target) break; }
        $label = $graph->get_label($host) . ' - ' . $graph->get_label($target);
        $graph->add_literal_triple($s, RDFS_LABEL, $label);
      }
    }

    $graph->skolemise_bnodes('http://lod-cloud.net/'.$packageName.'/');
    for ($i = 0; $i < 5; $i++) {
      // try five times
      $response =   $latcStore->mirror_from_url($uri, $graph->to_json());
      if($response['success']){
        echo "\n {$uri} mirrored to triple store. \n";
        break;
      }  
    }
    if(!$response['success']){
      error_log(date('c')."\t{$uri} was not mirrored to the triple store.\n", 3, 'import_errors.log');
      file_put_contents('failed_import.json', json_encode($response));
      die;
    } 
}


  if(count(array_keys($results)) < $pageSize){
    $continue = false;
  } else {
    $offset+=$pageSize;
  }


} //endwhile



require BASE_DIR.'/scripts/calculate_statistics.php';
//update last modifed time
//
touch('last_imported_from_ckan');
?>
