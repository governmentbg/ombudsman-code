<?php

use App\Models\MConfigType;
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddDataToMConfigTypeTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_config_type', function (Blueprint $table) {
            MConfigType::create([
                'CfT_name' => 'Common',
            ]);
            MConfigType::create([
                'CfT_name' => 'Buttons top',
            ]);
            MConfigType::create([
                'CfT_name' => 'Buttons center',
            ]);
            MConfigType::create([
                'CfT_name' => 'Social media',
            ]);
            MConfigType::create([
                'CfT_name' => 'Footer',
            ]);
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_config_type', function (Blueprint $table) {
            //
        });
    }
}
