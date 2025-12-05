Feature: Account management

    Scenario: Register user and validate account operations
        Given a logged in user with random email and password

        When person is updated with name 'John' and surname 'Wick' and person id 1
        Then the person update succeeds and account balance is 0.00
        
        When 25.50 'EUR' is added to the account
        Then the account balance is 25.50
        And the payment contains 25.50 'EUR'
