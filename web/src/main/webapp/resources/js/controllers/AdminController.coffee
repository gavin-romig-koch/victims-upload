define [], () ->
  ['$scope', '$http', 'notificationService', 'accountService', 'machineService', ($scope, $http, notify, accountService, machineService) ->
    # You can access the scope of the controller from here
    $scope.welcomeMessage = 'Red Hat PhD Admin'
    $scope.accounts = []
    $scope.accountMachines = []

    ####################################################################################################################
    # Test data
    ####################################################################################################################
    $scope.createTestData = (accountId) ->
      accountService.createTestData accountId, (data) ->
        notify.success "Successfully created test using accountId: #{accountId}"
        #$scope.refreshAccounts()

    $scope.deleteTestData = (accountId) ->
      accountService.deleteTestData accountId, (data) ->
        notify.success "Successfully deleted test data associated with account id: #{accountId}"
        #$scope.refreshAccounts()

    ####################################################################################################################
    # Account Crud
    ####################################################################################################################
    # Define a method to easily refresh all accounts -- TODO should limit to say 100
    $scope.refreshAccounts = () ->
      accountService.getAccounts (data) ->
        $scope.accounts = data

    $scope.saveAccount = (accountId) ->
      accountService.saveAccount accountId, (data) ->
        notify.success "Successfully saved account with id: #{accountId}"
        $scope.refreshAccounts()

    $scope.deleteAccount = (accountId) ->
      accountService.deleteAccount accountId, (data) ->
        notify.success "Successfully deleted account with id: #{accountId}"
        $scope.refreshAccounts()

    ####################################################################################################################
    # Machine Crud
    ####################################################################################################################
    $scope.refreshMachines = (accountId) ->

      machineService.getMachines accountId, (data) ->
        $scope.accountMachines = data


    $scope.saveMachine = (accountId, uuid) ->
      machineService.saveMachine accountId, uuid, (data) ->
        notify.success "Successfully saved machine with uuid: #{uuid} to account: #{accountId}"
        $scope.refreshMachines(accountId)

    $scope.deleteMachine = (accountId, uuid) ->
      machineService.deleteMachine accountId, uuid, (data) ->
        notify.success "Successfully deleted machine with id: #{uuid} from account: #{accountId}"
        $scope.refreshMachines(accountId)

    ####################################################################################################################
    # The following calls will be executed on this Controller instantiation
    ####################################################################################################################

    # Refresh accounts so onload there will be accounts showing up
    $scope.refreshAccounts()

    # because this has happened asynchroneusly we've missed
    # Angular's initial call to $apply after the controller has been loaded
    # hence we need to explicityly call it at the end of our Controller constructor
    $scope.$apply()

    # prevent "Error: [$parse:isecdom] Referencing DOM nodes in Angular expressions is disallowed! Expression:"
    # with an explicit return
    return
  ]
