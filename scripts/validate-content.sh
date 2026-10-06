#!/usr/bin/env bash
set -euo pipefail

content_file="app/src/main/assets/windchill_content.json"
if [[ ! -f "$content_file" ]]; then
  echo "Missing bundled curriculum: $content_file" >&2
  exit 1
fi

jq -e '
  . as $data
  | ($data.modules | length) > 0
    and ($data.lessons | length) > 0
    and ($data.quizzes | length) > 0
    and all($data.lessons[];
      (.id | type == "string")
      and (.title | type == "string")
      and (.sourceIds | type == "array")
      and (.releases | type == "array")
      and ((.moduleId) as $moduleId | any($data.modules[]; .id == $moduleId))
    )
    and all($data.quizzes[];
      (.q | type == "string")
      and (.options | type == "array")
      and (.a | type == "number")
      and (.a >= 0)
      and (.a < (.options | length))
      and (.why | type == "string")
    )
' "$content_file" >/dev/null

printf 'Curriculum OK: %s lessons, %s modules, %s practice checks.\n' \
  "$(jq '.lessons | length' "$content_file")" \
  "$(jq '.modules | length' "$content_file")" \
  "$(jq '.quizzes | length' "$content_file")"
