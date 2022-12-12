<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int    $Ul_id
 * @property int    $created_at
 * @property int    $deleted_at
 * @property int    $updated_at
 * @property string $Ul_name
 * @property string $Ul_value
 */
class ULevels extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'u_levels';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Ul_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'created_at', 'deleted_at', 'Ul_name', 'Ul_value', 'updated_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Ul_id' => 'int', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'Ul_name' => 'string', 'Ul_value' => 'string', 'updated_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
    public function eq_level()
    {
        return $this->hasMany(User::class, 'Ul_id');
    }
    public function ul_levels()
    {
        return $this->hasMany(ULevel2article::class, 'Ul_id');
    }
}
