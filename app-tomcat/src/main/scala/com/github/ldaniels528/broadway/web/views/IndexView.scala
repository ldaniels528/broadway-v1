package com.github.ldaniels528.broadway.web.views

/**
  * Index View
  */
trait IndexView {

  val indexView =
    <html ng-app="broadway">
      <head>
        <title>Broadway: Dashboard</title>
        <meta http-equiv="Content-type" content="text/html; charset=utf-8"/>
        <link href="/webjars/lib/bootstrap/css/bootstrap.min.css" type="text/css" rel="stylesheet" media="screen"/>
        <link href="/webjars/lib/highlightjs/styles/tomorrow.min.css" type="text/css" rel="stylesheet" media="screen"/>
        <link href="/assets/stylesheets/index.css" type="text/css" rel="stylesheet" media="screen"/>
        <script src="/webjars/jquery/2.1.3/jquery.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs/1.4.8/angular.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs/angular-animate.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs/angular-cookies.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/nervgh-angular-file-upload/angular-file-upload.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/highlightjs/highlight.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angular-highlightjs/angular-highlightjs.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs/angular-resource.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs/angular-route.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs/angular-sanitize.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/d3js/d3.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/nvd3/nv.d3.min.js" type="text/javascript"></script>
        <script src="/webjars/lib/angularjs-toaster/toaster.js" type="text/javascript"></script>
        <script src="/webjars/lib/angular-ui-bootstrap/ui-bootstrap-tpls.min.js" type="text/javascript"></script>
        <script src="/assets/javascripts/broadway_js-fastopt.js"></script>
        <script src="/assets/javascripts/broadway_js-launcher.js"></script>
      </head>
      <body id="TrifectaMain" ng-controller="MainController">
        <h2>
          <span class="title1">Bro</span> <span class="title2">ad</span> <span class="title3">way</span> <span class="version">v{{ version }}</span>
        </h2>
        <div class="title_border">
          <tabset>
            <tab ng-repeat="tab in tabs" active="tab.active" select="changeTab(tab, $event)">
              <tab-heading>
                <i ng-class="tab.icon">{{ tab.name }}</i>
              </tab-heading>
            </tab>
          </tabset>
        </div>
        <div ng-view="" ng-cloak=""></div>
      </body>
    </html>


  private def resource(path: String) = {
    Option(getClass.getResource(path)).map(_.toExternalForm).getOrElse(s"/$path")
  }

}
