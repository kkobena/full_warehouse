import 'package:flutter/material.dart';
import 'package:mobile/src/models/list_item.dart';
import 'package:mobile/src/utils/constant.dart';

Widget buildMetterGroup({
  required BuildContext context,
  required String title,
  required List<ListItem> data,
  VoidCallback? onTap,
}) {
  String firstLetter = Constant.getFirstLetter(title);
  final Color circleAvatarForegroundColor =
      Constant.getCircleAvatarForegroundColor(context);
  final Color circleAvatarBackgroundColor =
      Constant.getCircleAvatarBackgroundColor(context);

  return Card(
    margin: const EdgeInsets.all(4.0),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8.0)),
    elevation: 0.2,
    clipBehavior: Clip.antiAlias,
    child: Padding(
      padding: const EdgeInsets.all(10.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              CircleAvatar(
                radius: 20,
                backgroundColor: circleAvatarBackgroundColor,
                child: Text(
                  firstLetter,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: circleAvatarForegroundColor,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color:
                        Theme.of(context).textTheme.titleLarge?.color ??
                        Theme.of(context).colorScheme.onSurface,
                  ),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 2,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Divider(
            height: 1,
            color: Theme.of(context).dividerColor.withValues(alpha: 0.5),
          ),
          const SizedBox(height: 12),

          /// Meter bar
          Row(
            children: data
                .asMap() // Converts the List to a Map<int, ListItem> (index -> item)
                .entries // Gets an Iterable of MapEntry<int, ListItem>
                .map((entry) {
                  int index = entry.key;
                  ListItem item = entry.value;
                  Color itemColor =
                      Constant.metterGroupColors[index %
                          Constant.metterGroupColors.length];

                  return Expanded(
                    flex: (double.parse(item.autre ?? '0') * 100).toInt(),
                    child: Container(
                      height: 8,
                      color: itemColor, // Use the color from the list by index
                    ),
                  );
                })
                .toList(),
          ),
          const SizedBox(height: 12),

          /// metter list
          ...data.asMap().entries.map((entry) {
            int index = entry.key; // This is the index of the current item
            ListItem item = entry.value; // This is the current ListItem

            // Calculate color index, ensuring it wraps around if data.length > colors.length
            Color itemColor = Constant
                .metterGroupColors[index % Constant.metterGroupColors.length];
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4.0),
              child: Row(
                children: [
                  Container(
                    width: 10,
                    height: 10,
                    decoration: BoxDecoration(
                      color: itemColor,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 5),
                  Expanded(
                    child: RichText(
                      text: TextSpan(
                        text: item.libelle,
                        style: const TextStyle(
                          color: Colors.black,
                          fontSize: 14,
                        ),
                        children: [
                          TextSpan(
                            text: "(${item.autre}%)",
                            style: const TextStyle(color: Colors.grey),
                          ),
                        ],
                      ),
                    ),
                  ),
                  Text(
                    item.value,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    ),
  );
}
