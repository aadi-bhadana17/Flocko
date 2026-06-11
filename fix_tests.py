import os
import glob
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Apply all general mappings
    # Set owner
    content = re.sub(r'\.setOwner\(([^)]+)\)', r'.setOwnerUserId(\1.getId())', content)
    # user.setOwnedRestaurants(List.of(...)) -> we need to extract the IDs.
    # We can probably replace `.setOwnedRestaurants` with `.setOwnedRestaurantIds`.
    # It requires sets of longs. That might be a bit tricky if they used List.of(restaurant).
    # Since this is a bit complex in python regex, let's substitute setOwnedRestaurants to use stream map:
    # owner.setOwnedRestaurants(list) -> owner.setOwnedRestaurantIds(list.stream().map(Restaurant::getRestaurantId).collect(Collectors.toSet()))
    content = re.sub(r'(\w+)\.setOwnedRestaurants\(([^;]+)\);', r'\1.setOwnedRestaurantIds(\2.stream().map(com.kilgore.fooddeliveryapp.catalog.model.Restaurant::getRestaurantId).collect(java.util.stream.Collectors.toSet()));', content)

    # cart.setUser -> cart.setUserId
    content = re.sub(r'(\w+)\.setUser\(([^)]+)\)', r'\1.setUserId(\2.getId())', content)

    # cart.setRestaurant -> cart.setRestaurantId
    content = re.sub(r'(\w+)\.setRestaurant\(([^)]+)\)', r'\1.setRestaurantId(\2.getRestaurantId())', content)

    # food.setFoodCategory(cat) -> food.setCategoryId(cat.getCategoryId())
    content = re.sub(r'(\w+)\.setFoodCategory\(([^)]+)\)', r'\1.setCategoryId(\2.getCategoryId())', content)

    # item.setFood(food) -> item.setFoodId(food.getFoodId())
    content = re.sub(r'(\w+)\.setFood\(([^)]+)\)', r'\1.setFoodId(\2.getFoodId())', content)

    # item.setAddons(addons) -> item.setAddonIds(addons.stream().map(Addon::getAddonId).collect(Collectors.toList()))
    content = re.sub(r'(\w+)\.setAddons\(([^;]+)\);', r'\1.setAddonIds(\2.stream().map(com.kilgore.fooddeliveryapp.catalog.model.Addon::getAddonId).collect(java.util.stream.Collectors.toList()));', content)

    # user.setOrders(orders) -> user.setOrderIds(orders.stream().map(Order::getOrderId).collect(Collectors.toList()))
    content = re.sub(r'(\w+)\.setOrders\(([^;]+)\);', r' ', content) # it turns out User doesn't have orders list directly in module monolith

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

test_files = glob.glob('src/test/java/**/*.java', recursive=True)
for f in test_files:
    process_file(f)

print("Replacement done.")

