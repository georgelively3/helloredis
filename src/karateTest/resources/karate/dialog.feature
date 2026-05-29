Feature: Dialog CRUD via Redis

  Background:
    * url baseUrl

  Scenario: Create a dialog
    Given path '/api/dialogs'
    And request { id: 100, request: 'hello', response: 'world' }
    When method POST
    Then status 201
    And match response.id == 100
    And match response.request == 'hello'

  Scenario: Get all dialogs contains the created dialog
    Given path '/api/dialogs'
    When method GET
    Then status 200
    And match response[*].id contains 100

  Scenario: Get dialog by id and matching request
    Given path '/api/dialogs/100'
    And param request = 'hello'
    When method GET
    Then status 200
    And match response.response == 'world'

  Scenario: Get dialog with wrong request returns 404
    Given path '/api/dialogs/100'
    And param request = 'wrong'
    When method GET
    Then status 404

  Scenario: Update dialog
    Given path '/api/dialogs/100'
    And request { id: 100, request: 'hello', response: 'updated' }
    When method PUT
    Then status 200
    And match response.response == 'updated'

  Scenario: Delete dialog
    Given path '/api/dialogs/100'
    When method DELETE
    Then status 204

  Scenario: Get deleted dialog returns 404
    Given path '/api/dialogs/100'
    And param request = 'hello'
    When method GET
    Then status 404
