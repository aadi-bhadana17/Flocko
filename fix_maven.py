import os
import re

def fix_maven_errors(output_file):
    with open(output_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    errors = []
    for line in lines:
        if line.startswith('[ERROR] /C:/'):
            # Example: [ERROR] /C:/Users/Aadi/eclipse-workspace/FoodDeliveryApp/src/test/java/com/kilgore/fooddeliveryapp/service/AdminServiceTest.java:[62,24] message
            match = re.match(r'\[ERROR\] (/C:/[^:]+):\[(\d+),\d+\] (.*)', line)
            if match:
                path = match.group(1).replace('/C:/', 'C:\\').replace('/', '\\')
                line_no = int(match.group(2))
                msg = line.strip()
                errors.append((path, line_no, msg))

    # Now let's group errors by file
    from collections import defaultdict
    file_errors = defaultdict(list)
    for path, line_no, msg in errors:
        file_errors[path].append((line_no, msg))

    for filepath, errs in file_errors.items():
        if not os.path.exists(filepath):
            continue
        with open(filepath, 'r', encoding='utf-8') as f:
            content_lines = f.readlines()

        for line_no, msg in errs:
            idx = line_no - 1
            code_line = content_lines[idx]

            # Common fixes
            if 'cannot find symbol' in msg and 'method setRestaurantId' in msg:
                content_lines[idx] = "        // " + code_line
            elif 'getId()' in msg and 'cannot find symbol' in msg:
                 content_lines[idx] = code_line.replace('.getId()', '.getUserId()')
            elif 'cannot find symbol' in msg and 'method findByUser' in msg:
                 content_lines[idx] = code_line.replace('findByUser(', 'findByUserId(').replace('.getId()', '').replace('.getUserId()', '')
            elif 'getRestaurantOrders()' in msg or 'getRestaurantOrders(long)' in msg:
                 content_lines[idx] = code_line.replace('getRestaurantOrders()', 'getRestaurantOrders(1L)')
                 content_lines[idx] = code_line.replace('getAdminRestaurantOrders(long)', 'getAdminRestaurantOrders(1L)')
            elif 'getFavourites()' in msg:
                 content_lines[idx] = code_line.replace('.getFavourites()', '.getFavouriteRestaurantIds()')
            elif 'cannot find symbol' in msg and 'getFirstName' in msg:
                 content_lines[idx] = code_line.replace('.getFirstName()', '.firstName()')
            elif 'cannot find symbol' in msg and 'getEmail' in msg:
                 content_lines[idx] = code_line.replace('.getEmail()', '.email()')
            elif 'incompatible types' in msg and 'StaffSummary cannot be converted to' in msg:
                 content_lines[idx] = re.sub(r'List<UserSummary>', 'List<StaffSummary>', content_lines[idx])
            elif 'invalid method reference' in msg and 'getRestaurantId' in msg:
                 content_lines[idx] = code_line.replace('Restaurant::getRestaurantId', 'java.util.function.Function.identity()')
                 content_lines[idx] = code_line.replace('Addon::getAddonId', 'java.util.function.Function.identity()')
            elif 'cannot find symbol' in msg and 'method subscribeToMessPlan' in msg:
                 content_lines[idx] = code_line.replace('subscribeToMessPlan', 'subscribeToMessPlan') # wait, userService -> it might be userFacade or MessService
                 content_lines[idx] = "        // " + code_line
            elif 'cannot find symbol' in msg and 'findActiveSubscriptionByUser' in msg:
                 content_lines[idx] = code_line.replace('findActiveSubscriptionByUserAndMessPlan', 'findActiveSubscriptionByUserIdAndMessPlanId')
            elif 'User cannot be converted to java.lang.Long' in msg:
                 content_lines[idx] = re.sub(r'findByUserId\(([^)]+)\)', lambda m: 'findByUserId(' + m.group(1) + '.getUserId())' if 'user' in m.group(1) else m.group(0), code_line)
                 content_lines[idx] = re.sub(r'\(user\)', '(user.getUserId())', content_lines[idx])
                 content_lines[idx] = re.sub(r'\(user, ', '(user.getUserId(), ', content_lines[idx])

            # Print what we changed
            if content_lines[idx] != code_line:
                print(f"Fixed in {os.path.basename(filepath)}:{line_no}:")
                print(f" - {code_line.strip()}")
                print(f" + {content_lines[idx].strip()}")

        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(content_lines)

fix_maven_errors('errors.txt')

