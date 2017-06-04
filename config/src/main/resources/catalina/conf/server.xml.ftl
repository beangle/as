[#ftl]
<?xml version='1.0' encoding='utf-8'?>
<Server port="${server.shutdownPort}" shutdown="SHUTDOWN">
  [#list container.listeners as listener]
  <Listener className="${listener.className}" [#list listener.properties?keys as k]${k}="${listener.properties[k]}"[/#list]/>
  [/#list]

  [#if container.resourceNames?size >0 ]
  <GlobalNamingResources>
  [#list container.farmResourceNames(farm)?sort as resourceName]
    [#assign resource=container.resources[resourceName]/]
    <Resource name="${resource.name}"
      [#list resource.properties?keys as p]
      ${p}="${resource.properties[p]}"
      [/#list]
    />
 [/#list]
  </GlobalNamingResources>
 [/#if]

  <Service name="Catalina">
    [#if server.http??]
    [#assign http=server.http/]
    <Connector port="${server.httpPort}" protocol="HTTP/1.1"
      URIEncoding="${http.URIEncoding}"
      enableLookups="${http.enableLookups?c}"
      [#if http.redirectPort??]
      redirectPort="${http.redirectPort}"
      [/#if]
      acceptCount="${http.acceptCount}"
      maxThreads="${http.maxThreads}"
      minSpareThreads="${http.minSpareThreads}"
      [#if http.redirectPort??]
      redirectPort="${http.redirectPort}"
      [/#if]
      connectionTimeout="${http.connectionTimeout}"
      disableUploadTimeout="${http.disableUploadTimeout?c}"
      [#if http.compression!="off"]
      compression="${http.compression}"
      compressionMinSize="${http.compressionMinSize}"
      compressableMimeType="${http.compressionMimeType}"
      [#else]
      compression="off"
      [/#if]
      />
    [/#if]

    [#if server.ajp??]
    [#assign ajp=server.ajp/]
    <Connector port="${server.ajpPort}" protocol="AJP/1.3"
      URIEncoding="${ajp.URIEncoding}"
      enableLookups="${ajp.enableLookups?c}"
      acceptCount="${ajp.acceptCount}"
      maxThreads="${ajp.maxThreads}"
      minSpareThreads="${ajp.minSpareThreads}"
      [#if ajp.redirectPort??]redirectPort="${ajp.redirectPort}"[/#if]
      />
    [/#if]
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false">
      [#list container.deployments as deployment]
      [#if deployment.matches(server.qualifiedName)]
      [#list container.webapps as webapp]
      [#if webapp.name == deployment.webapp]
      <Context path="${deployment.path}" reloadable="${webapp.reloadable?c}"[#rt/]
      [#lt/][#if webapp.docBase??] docBase="${webapp.docBase}"[/#if][#rt/]
      [#lt/][#list webapp.properties?keys as p] ${p}="${webapp.properties[p]}"[/#list]>
        [#list webapp.resources as resource]
        <ResourceLink name="${resource.name}" global="${resource.name}" type="${resource.type}" />
        [/#list]
        [#if container.context??]
        [#assign ctx=container.context/]
        [#if ctx.jarScanner??]<JarScanner [@spawnProps ctx.jarScanner/]/>[/#if]
        [#if ctx.loader??]<Loader className="${ctx.loader.className}" [@spawnProps ctx.loader/]/>[/#if]
        [/#if]
        ${webapp.realms!}
        </Context>
       [/#if]
       [/#list]
       [/#if]
       [/#list]
      </Host>
    </Engine>
  </Service>
</Server>
[#-- display object properties--]
[#macro spawnProps obj][#list obj.properties?keys as k] ${k}="${obj.properties[k]}" [/#list][/#macro]
