// Runs inside the Threat Dragon image: finds the schema of the v2 model format in the editor's own bundle, compiles it with the Ajv that the image ships, and checks the given files.
const fs = require('fs');
const dir = '/app/dist/js';
let schema = null;
for (const f of fs.readdirSync(dir).filter(n => n.endsWith('.js'))) {
  const s = fs.readFileSync(dir + '/' + f, 'utf8');
  const at = s.indexOf('threat-dragon-v2.schema.json');
  if (at < 0) continue;
  const start = s.lastIndexOf("JSON.parse('", at) + "JSON.parse('".length;
  const end = s.indexOf("')", at);
  schema = JSON.parse(s.slice(start, end).replace(/\\'/g, "'"));
  break;
}
const Ajv = require('/app/td.server/node_modules/ajv');
const validate = new Ajv({ allErrors: true, nullable: true, strict: false }).compile(schema);
const out = {};
for (const file of process.argv.slice(2)) {
  const ok = validate(JSON.parse(fs.readFileSync(file, 'utf8')));
  out[file.split('/').pop()] = { valid: ok, errors: (validate.errors || []).map(e => (e.instancePath || e.dataPath || '/') + ' ' + e.message) };
}
console.log(JSON.stringify(out));
