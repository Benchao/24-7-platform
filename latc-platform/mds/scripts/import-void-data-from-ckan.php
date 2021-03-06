<?php
require 'inc.php';

$lodTopics = getLodTopics();
  


$latcStore = new Store(LATC_STORE_URI, new Credentials('latc', LATC_STORE_PASSWORD));

$SparqlEndpoint = new SparqlService("http://semantic.ckan.net/sparql/");


$Prefixes = getPrefixes();

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
  {
    ?s <http://moat-project.org/ns#taggedWithTag> <http://ckan.net/tag/lod> .
  } UNION 
  {
    ?s <http://moat-project.org/ns#taggedWith> <http://ckan.net/tag/lod> .
  }
   {$modifiedClause}
}
LIMIT {$pageSize} OFFSET {$offset}
_SPARQL_;

echo "Running SPARQL query for recently changed datasets \n";
$requests = array();
$results = $SparqlEndpoint->select_to_array($query);

$count = count($results); 

echo "Got $count results \n";

$uri_prefixes_to_replace = array(
  'http://ckan.net/package',
  'http://ckan.net/tag',
  'http://thedatahub.org/dataset',
  'http://thedatahub.org/package',
  'http://thedatahub.org/tag',
);

$RequestFactory = new HttpRequestFactory();

$vocabGraph = new SimpleGraph();
foreach($results as $row){
  $uri = $row['s']['value'];
  $request = $RequestFactory->make("GET", $uri);
  $request->set_accept('application/rdf+xml');
  $response = $SparqlEndpoint->query('DESCRIBE <'.$uri.'>', 'application/rdf+xml');
  //$response = $request->execute();
    if(!$response->is_success()){
      echo "Could not fetch description for {$uri} \n";
      continue;
    } else {
      echo "fetched $uri \n";
    }
    $graph = new SimpleGraph();
    $graph->add_rdf($response->body);
    $graph->add_resource_triple($uri, RDF_TYPE, 'http://rdfs.org/ns/void#Dataset');
    $graph->remove_resource_triple($uri, RDF_TYPE, 'http://www.w3.org/ns/dcat#Dataset');
    if($title=$graph->get_first_literal($uri, DCT.'title', null, 'en')){
      $graph->remove_literal_triple($uri, DCT.'title',$title);
      $graph->add_literal_triple($uri, RDFS_LABEL, $title);
    }
    $graph->add_literal_triple($uri, OPENVOCAB.'canonicalUri', $uri);
    if($ckanJSON = $graph->get_first_literal($uri, 'http://semantic.ckan.net/schema#json')){
      $graph->remove_literal_triple($uri,  'http://semantic.ckan.net/schema#json', $ckanJSON);
    }
    if($ckanExtras = $graph->get_resource_triple_values($uri, 'http://semantic.ckan.net/schema#extra')){
      foreach($ckanExtras as $extraUri){
        $graph->remove_resource_triple($uri,  'http://semantic.ckan.net/schema#extra', $extraUri);
      }
    }
    if($tokens = $graph->get_literal_triple_values($uri, 'http://4store.org/fulltext#token')){
      $graph->remove_property_values($uri, 'http://4store.org/fulltext#token');
    }
  
    if($tags = $graph->get_resource_triple_values($uri, MOAT.'taggedWithTag')){
      $graph->remove_property_values($uri, MOAT.'taggedWithTag');
      foreach($tags as $tag){
        $graph->add_resource_triple($uri, MOAT.'taggedWith', $tag);
      }
    }

    

    $ckanArray = json_decode($ckanJSON, true);

    if(isset($ckanArray['extras']['sparql_graph_name'])){
//      $namedGraphUri = $uri.'/sparql-graph';
  //    $graph->add_resource_triple($uri, SSD.'namedGraph', $namedGraphUri);
      $graph->add_literal_triple($uri, SSD.'name', $ckanArray['extras']['sparql_graph_name']);
 //     $graph->add_resource_triple($namedGraphUri, RDF_TYPE, SSD.'Graph');
 //     $graph->add_literal_triple($namedGraphUri, RDFS_LABEL, $ckanArray['extras']['sparql_graph_name']);
    }

   $graphURIs = array();
    foreach($graph->get_index() as $s => $ps){
      foreach($uri_prefixes_to_replace as $prefix_replace){
        if(strpos($s,$prefix_replace)===0){
          $graphURIs[]=$s;
        }
      }
      foreach($ps as $p => $os){
        foreach($os as $o){
          if($o['type']=='uri'){
           foreach($uri_prefixes_to_replace as $prefix_replace){
              if(strpos($o['value'],$prefix_replace)===0){
                  $graphURIs[]=$o['value'];
              }
           }

          }
        }
      }
    }
  $graphURIs = array_unique($graphURIs);
    foreach($graphURIs as $oldUri){
      foreach($uri_prefixes_to_replace as $prefix){
        if(strpos($oldUri, $prefix)===0){
          $lodCloudUri = str_replace('http://ckan.net/','http://lod-cloud.net/', $oldUri);
          $lodCloudUri = str_replace('http://thedatahub.org/','http://lod-cloud.net/', $lodCloudUri);
          $lodCloudUri = str_replace('package/', '/', $lodCloudUri);
          break;
        }
      }
      $graph->replace_resource($oldUri, $lodCloudUri);
      $graph->add_resource_triple($lodCloudUri, OWL_SAMEAS, $oldUri);
    }

    $packageName = str_replace('http://ckan.net/package/', '', $uri);
    $packageName = str_replace('http://thedatahub.org/package/', '', $uri);
    $packageName = str_replace('http://thedatahub.org/dataset/', '', $uri);

    $uri = LOD.'dataset/'.$packageName;

    $graph->add_literal_triple($uri, OPENVOCAB.'shortName', $packageName);

       if(isset($ckanArray['extras'])){
      if(isset($ckanArray['extras']['uriSpace'])){
        $graph->add_literal_triple($uri, VOID.'uriSpace', $ckanArray['extras']['uriSpace']);
      }
      else if(isset($ckanArray['extras']['namespace'])){
        $graph->add_literal_triple($uri, VOID.'uriSpace', $ckanArray['extras']['namespace']);
      } else if(
        $uriInDataset = $graph->get_first_resource($uri, VOID.'exampleResource')
        OR
        $uriInDataset = $graph->get_first_resource($uri, FOAF.'homepage')
      ){
        if(preg_match('@http://[^/]+/*@', $uriInDataset, $m)){
          $graph->add_literal_triple($uri, VOID.'uriSpace', $m[0]);
        }
      }

      if($uriSpace = $graph->get_first_literal($uri, VOID.'uriSpace')){
        $host = parse_url($uriSpace, PHP_URL_HOST);
        $graph->add_literal_triple($uri, DSI.'thirdLevelDomain', $host);
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

    if(isset($ckanArray['url'])){
      $graph->add_resource_triple($uri, FOAF.'homepage', $ckanArray['url']);
    }

    if(isset($ckanArray['tags'])){
      foreach($ckanArray['tags'] as $tag){
        if(strpos($tag, 'format-')===0){
          $prefix = str_replace('format-', '', $tag);
          if(isset($Prefixes[$prefix])){
            $vocabUri = rtrim($Prefixes[$prefix], '#');
            $vocabGraph->add_literal_triple($vocabUri, RDFS_LABEL, $prefix);
            $vocabGraph->add_literal_triple($vocabUri, VANN.'preferredNamespacePrefix', $prefix);
            $vocabGraph->add_literal_triple($vocabUri, VANN.'preferredNamespaceUri', $Prefixes[$prefix]);
            $vocabGraph->add_resource_triple($uri, VOID.'vocabulary', $vocabUri);
          }
        } else if(in_array($tag, $lodTopics)) {
            $graph->add_resource_triple($uri, DCT.'subject', lodThemes.$tag);
        }
        
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

    $graph->skolemise_bnodes('http://lod-cloud.net/dataset/'.$packageName.'/');
    $graphName = 'http://lod-cloud.net/'. $packageName;

   for ($i = 0; $i < 5; $i++) {
      // try five times
      $response =   $latcStore->mirror_from_url($graphName, $graph->to_json());
      if($response['get_copy']->status_code=='200'){
        echo "\n Copy of {$uri} found in content box. ";
      } else if($response['get_copy']->status_code=='404') {
        echo "\n Copy of {$uri} not found in content box";
      } else {
        echo "\n there was a problem retrieving a copy of {$uri} from the content box";
      }
      if($response['update_data']){
        switch($response['update_data']->is_success()){
          case true:
            echo "\n triples from {$uri} were modified successfully";
            break;
          case false:
            echo "\n there was a problem modifying the triples from {$uri}";
            var_dump($response['update_data']);
        }
      }

      if($response['success']){
        echo "\n {$uri} mirrored to triple store. \n";
        break;
      }  
    }
    if(!$response['success']){
      error_log(date('c')."\t{$uri} was not mirrored to the triple store.\n", 3, 'import_errors.log');
      file_put_contents('failed_import.json', json_encode($response));
    } 
}


  if(count(array_keys($results)) < $pageSize){
    $continue = false;
  } else {
    $offset+=$pageSize;
  }


} //endwhile


$r = $latcStore->mirror_from_url(LOD.'vocabularies', $vocabGraph->to_turtle());

var_dump($r['success']);

require BASE_DIR.'/scripts/calculate_statistics.php';
//update last modifed time
//
touch('last_imported_from_ckan');
?>
