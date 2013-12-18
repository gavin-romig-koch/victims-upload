define ['underscore', 'd3Logic'], (_) ->
  ['$scope', '$http', '$routeParams', 'notificationService', 'accountService', 'machineService', 'metricService', 'sharedProperties', ($scope, $http, $routeParams, notify, accountService, machineService, metricService, sharedProperties) ->
    # You can access the scope of the controller from here
    $scope.welcomeMessage = 'Machine Controller'

    ####################################################################################################################
    # Route Handling
    ####################################################################################################################
    sharedProperties.handleRouteParams($routeParams)
    # The shared properties comes from the service.js
    $scope.sharedProperties = sharedProperties

    ####################################################################################################################
    # Handle the initial account dashboard
    ####################################################################################################################

    # TODO this needs someway to callback to a directive method so I can autoupdate
    # Create a proxy method that allows async fetching of metrics directly to the d3 directive
    $scope.getD3MetricValues = (machine, metricName, beginDate, endDate) ->
      metricService.getMetricValues machine.accountId, machine.orgId, machine.uuid, metricName, beginDate, endDate, (data) ->
        console.log "Received: #{data}"
    # Transform to a d3 format


    # because this has happened asynchroneusly we've missed
    # Angular's initial call to $apply after the controller has been loaded
    # hence we need to explicityly call it at the end of our Controller constructor
    $scope.$apply()

    # prevent "Error: [$parse:isecdom] Referencing DOM nodes in Angular expressions is disallowed! Expression:"
    # with an explicit return
    return
  ]
