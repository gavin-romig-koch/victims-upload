define ['underscore', 'd3Logic'], (_, d3Logic) ->
  ['$scope', '$http', '$routeParams', 'notificationService', 'accountService', 'machineService', 'metricService', 'sharedProperties', ($scope, $http, $routeParams, notify, accountService, machineService, metricService, sharedProperties) ->
    # You can access the scope of the controller from here
    $scope.welcomeMessage = 'Red Hat PhD Admin'
    # Contains a list of machines for the account containing accountId and orgId embedded
    $scope.denormMachines = []

    ####################################################################################################################
    # Route Handling
    ####################################################################################################################
    sharedProperties.handleRouteParams($routeParams)
    # The shared properties comes from the service.js
    $scope.sharedProperties = sharedProperties

    ####################################################################################################################
    # Handle the initial account dashboard
    ####################################################################################################################

    # Whenever the account changes, de-normalize the machines
    # TODO In the future if I want this to mirror the grouping of the radio, easily done with a few minor changes
#    $scope.$watch sharedProperties.accountData, (newVal) ->
#      tmp = []
#      _.each newValue?.organizations, (org) ->
#        _.each org.machines, (machine) ->
#            obj =
#              accountId: newValue.accountId
#              orgId: org.orgId
#              orgName: org.orgName
#              uuid: machine.uuid
#              hostname: machine.hostname
#            # Push the machine to the array unless it is already there.  I think it can be with multiple orgs
#            tmp.push obj unless _.find tmp, {uuid: machine.uuid}
#
#      $scope.denormMachines = tmp

    # TODO this needs someway to callback to a directive method so I can autoupdate
    # Create a proxy method that allows async fetching of metrics directly to the d3 directive
    $scope.getD3MetricValues = (machine, metricName, beginDate, endDate) ->
      metricService.getMetricValues machine.accountId, machine.orgId, machine.uuid, metricName, beginDate, endDate, (data) ->
        console.log "Received: #{data}"
        # Transform to a d3 format

    ####################################################################################################################
    # D3 conversions
    ####################################################################################################################
    $scope.$watch "groupAccountBy", (newVal) ->
      d3Logic.accountToTree {accountId: $scope.sharedProperties.accountId, data: $scope.sharedProperties.accountData, groupBy: newVal}, (result) ->
        $scope.sharedProperties.accountTree = result


    # because this has happened asynchroneusly we've missed
    # Angular's initial call to $apply after the controller has been loaded
    # hence we need to explicityly call it at the end of our Controller constructor
    $scope.$apply()

    # prevent "Error: [$parse:isecdom] Referencing DOM nodes in Angular expressions is disallowed! Expression:"
    # with an explicit return
    return
  ]
