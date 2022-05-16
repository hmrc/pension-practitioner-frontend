
# Pensions Practitioner Frontend

## Info

This service allows a user to register as a pension practitioner.

This service has a corresponding back-end service, namely pension-practitioner which integrates with HOD i.e DES/ETMP.

### Dependencies

| Service                     | Link                                                |
|-----------------------------|-----------------------------------------------------|
| Pensions Scheme             | https://github.com/hmrc/pensions-scheme             |
| Pension Practitioner        | https://github.com/hmrc/pension-practitioner        |
| Address Lookup              | https://github.com/hmrc/address-lookup              |
| Email                       | https://github.com/hmrc/email                       |
| Auth                        | https://github.com/hmrc/auth                        |
| Identity Verification       | https://github.com/hmrc/identity-verification       |
| Identity Verification Proxy | https://github.com/hmrc/identity-verification-proxy |
| Tax Enrolments              | https://github.com/hmrc/tax-enrolments              |


### Endpoints used

| Service                     | HTTP Method | Route                                                                                     | Purpose                                                                                                   |
|-----------------------------|-------------|-------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Pension Practitioner        | POST        | ```/pension-practitioner/register-with-id/individual                                  ``` | Registers an individual based on the NINO from ETMP                                                       |
| Pension Practitioner        | POST        | ```/pension-practitioner/register-with-no-id/organisation                             ``` | Registers an organisation on ETMP who does not have a UTR. Typically this will be a non- UK organisation  |
| Pension Practitioner        | POST        | ```/pension-practitioner/register-with-no-id/individual                               ``` | Registers an individual on ETMP who does not have a UTR/NINO. Typically this will be a non- UK individual |
| Pension Practitioner        | POST        | ```/pension-practitioner/subscribePsp/:journeyType                                    ``` | Subscribe a pension scheme practitioner                                                                   |
| Pension Practitioner        | GET         | ```/pension-practitioner/getPsp                                                       ``` | Get Psp subscription details                                                                              |
| Pension Practitioner        | POST        | ```/pension-practitioner/deregisterPsp/:pspId                                         ``` | De-register a Psp                                                                                         |
| Pension Practitioner        | POST        | ```/pension-practitioner/authorise-psp                                                ``` | Authorise a Psp                                                                                           |
| Pension Practitioner        | POST        | ```/pension-practitioner/de-authorise-psp                                             ``` | De-authorise a Psp                                                                                        |
| Pension Practitioner        | GET         | ```/pension-practitioner/get-minimal-details                                    ```       | Get minimal Psp details                                                                                   |
| Pension Practitioner        | GET         | ```/pension-practitioner/can-deregister/:id                                     ```       | Can de-register a Psp                                                                                     |
| Address Lookup              | GET         | ```/v2/uk/addresses ```                                                                   | Returns a list of addresses that match a given postcode                                                   | 
| Email                       | POST        | ```/hmrc/email```                                                                         | Sends an email to an email address                                                                        | 
| Identity Verification Proxy | POST        | ```/identity-verification-proxy/journey/start    ```                                      | Store IV Journey Data and generates a link that can be used to start IV Process                           | 
| Identity Verification       | GET         | ```/identity-verification/journey/:journeyId  ```                                         | Return the journey data e.g NINO for the given journey id                                                 | 
| Tax Enrolments              | POST        | ```/tax-enrolments/service/:serviceName/enrolment ```                                     | Enrols a user synchronously for a given service name                                                      | 


## Running the service

Service Manager: PODS_ALL

Port: 8208

Link: http://localhost:8208/

Enrolment key: HMRC-PODS-ORG

## Tests and prototype

| Repositories  | Link                                       |
|---------------|--------------------------------------------|
| Journey tests | https://github.com/hmrc/pods-journey-tests |
| Prototype     | https://pods-prototype.herokuapp.com/      |
