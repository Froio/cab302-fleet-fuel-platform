User Stories
1. As a user, I want to log in with a username and password so I can access the application.
The log in needs a username and password – If the entry is invalid it must prompt an error message without granting the incorrect identification access – A successful login directs to a dashboard based on role.
2. As a user, you want to log out of the application.
Logout option must be able to be seen on the application screen – Logging out takes you to the login page.
3. A fleet manager wants to make and manage accounts of users and control role based access.
Fleet manager must be able to make new accounts based on roles like driver or manager – can create and delete account.
4. The fleet manager wants to create vehicle data records and make vehicles registered into the fleet system.
Vehicle ID, Rego, Make, Model, Fuel Type, Current – Record saved appears in vehicle list – Reject duplicate numbers
5. Fleet Manager can edit or remove vehicle records to keep data accurate.
Update or save current vehicle data – Delete current vehicle accounts – Changes reflect in the vehicle list.
6. Fleet manager can assign a driver to a vehicle so the fuel logs reflect driver behaviour.
Fleet manager can select a driver to an existing vehicle – Vehicle assignments can be changed.
7. A driver wants to see details of vehicles assigned to them and see what they must do.
Driver sees vehicles assigned to them – Details of car must include rego, make, model, year, odometer – Only access assigned vehicles.
8. As a driver, I want to log my fuel fill ups to be able to track fuel usage and associated cost.
Form requires litres, cost, odometer – Odometer reading must be greater than the last recorded amount – Have a sort of log history for fuel logs.
9. As a driver, I want to be able to view the fuel history so I can review previous fill up data.
All past fuel logs are present for the driver's vehicle and in recent to old order – Entries show date, cost, litres and odometer – history updates immediately after logs are updated
10. As a driver, I want to be able to see the vehicle I'm driving fuel efficiency so I can analyse if anything is wrong with the vehicle.
Efficiency to be shown in L/100km, calculated from consecutive fuel logs – Efficiency updates after each fill up – a simple trend chart for analysis.
11. As a driver, I want to see the cost of each fill up and my cost per km so I can understand my running expenses.
Cost per fill up must be calculated and displayed – cost per km calculated using logged fuel and distance driven data – Value updates as new logs are added.
12. A fleet manager wants to view fuel and emissions reports across the whole fleet so that they can identify cost saving opportunities.
Report shows cost, efficiency and emissions per vehicle – data can be filtered – fleet managers can view reports for any vehicle not just their own.
13. A fleet manager wants to compare emissions across multiple vehicles so they can identify the most to least efficient vehicles.
Multiple vehicle emission reports displayed – emission calculated as litres – sortable by emissions.
14. As a driver, I want to export the fuel logs to CSV so I can keep personal records or submit them for tax purposes.
Export button available on fuel log data – csv includes date, litres, cost, efficiency, odometer – downloaded file option
15. As a fleet manager, I want to view a list of all drivers in the system, so I can see who's available to assign to vehicles.
All driver accounts are listed with name and current vehicle assignment status – List shows whether each driver is currently assigned or unassigned – List updates immediately when a driver is assigned/unassigned elsewhere in the system.
16. As a user, I want to update my own profile details (name, contact email), so my account information stays current.
Profile edit screen accessible from within the logged-in application – Fields for name and contact email can be updated and saved – Changes are reflected immediately and persist after logout/login.
17. As a fleet manager, I want to see which vehicles are currently unassigned to any driver, so I know what's available to allocate.
Vehicle list can be filtered to show only unassigned vehicles – Unassigned vehicles are clearly marked/separated from assigned ones – List updates immediately once a vehicle is assigned to a driver.
18. As a driver, I want to delete a fuel log entry I made by mistake, so my history stays accurate.
Delete option available on each individual fuel log entry – Confirmation prompt shown before deletion to prevent accidental removal – Deleted entry is removed from history and no longer included in efficiency/cost calculations.
19. As a fleet manager, I want to see the date each vehicle was added to the fleet, so I can track how long vehicles have been in service.
Date added is automatically recorded when a vehicle record is created (not manually entered) – Date is visible on the vehicle's detail view – Date remains unchanged even if other vehicle details are later edited.


